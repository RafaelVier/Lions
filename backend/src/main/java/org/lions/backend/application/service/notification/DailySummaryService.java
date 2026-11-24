package org.lions.backend.application.service.notification;

import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.response.DailySummaryResponse;
import org.lions.backend.application.dto.response.DailySummaryResponse.ExpiredReservation;
import org.lions.backend.application.dto.response.DailySummaryResponse.OpenMaintenance;
import org.lions.backend.application.dto.response.DailySummaryResponse.OverdueLoan;
import org.lions.backend.application.service.maintenance.PreventiveMaintenanceService;
import org.lions.backend.application.service.waitlist.WaitlistService;
import org.lions.backend.domain.entity.AuditLog;
import org.lions.backend.domain.entity.Equipment;
import org.lions.backend.domain.entity.Maintenance;
import org.lions.backend.domain.entity.WaitlistEntry;
import org.lions.backend.domain.enums.LoanStatus;
import org.lions.backend.domain.enums.MaintenanceStatus;
import org.lions.backend.domain.enums.NotificationType;
import org.lions.backend.domain.enums.WaitlistStatus;
import org.lions.backend.domain.repository.AuditLogRepositoryPort;
import org.lions.backend.domain.repository.LoanRepositoryPort;
import org.lions.backend.domain.repository.MaintenanceRepositoryPort;
import org.lions.backend.domain.repository.WaitlistEntryRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/** Resumo diário ao Administrador (RN49), consultável a qualquer momento e enviado pela rotina diária. */
@Service
@RequiredArgsConstructor
public class DailySummaryService {

    public static final String REFERENCE_KEY = "DAILY_SUMMARY";
    /** Itens listados por categoria na mensagem (a lista completa fica em {@code GET /notificacoes/resumo-diario}). */
    static final int MAX_ITEMS_IN_MESSAGE = 5;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LoanRepositoryPort loanRepository;
    private final WaitlistEntryRepositoryPort waitlistRepository;
    private final MaintenanceRepositoryPort maintenanceRepository;
    private final AuditLogRepositoryPort auditLogRepository;
    private final PreventiveMaintenanceService preventiveMaintenanceService;
    private final NotificationService notificationService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DailySummaryResponse build() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        return new DailySummaryResponse(today, now, overdueLoans(today), expiredReservations(now),
                openMaintenances(today), preventiveMaintenanceService.findDue());
    }

    /** Envia o resumo do dia aos administradores (notificação interna), no máximo uma vez por dia. */
    @Transactional
    public boolean sendToAdmins() {
        DailySummaryResponse summary = build();
        return notificationService.notifyAdmins(NotificationType.RESUMO_DIARIO, message(summary), REFERENCE_KEY,
                summary.date());
    }

    static String message(DailySummaryResponse s) {
        StringBuilder sb = new StringBuilder("Resumo diário de ").append(DATE.format(s.date())).append(':');
        if (s.isEmpty()) {
            return sb.append(" nenhuma pendência (sem atrasos, reservas expiradas, manutenções em aberto ou ")
                    .append("revisões preventivas vencidas).").toString();
        }
        section(sb, s.overdueLoans(), "empréstimo(s) em atraso", l -> l.beneficiaryName() + " — "
                + l.patrimonyNumber() + ", " + l.daysOverdue() + " dia(s) de atraso");
        section(sb, s.expiredReservations(), "reserva(s) expirada(s) nas últimas 24 h", r -> r.beneficiaryName()
                + " — " + r.equipmentType().getDescription() + (r.removedFromWaitlist() ? " (removido da fila)" : ""));
        section(sb, s.openMaintenances(), "manutenção(ões) em aberto", m -> m.patrimonyNumber() + " — há "
                + m.daysOpen() + " dia(s), " + m.technicalResponsible());
        section(sb, s.preventiveDue(), "revisão(ões) preventiva(s) vencida(s)", p -> p.patrimonyNumber() + " — "
                + p.reason());
        return sb.toString();
    }

    private static <T> void section(StringBuilder sb, List<T> items, String label, Function<T, String> line) {
        sb.append("\n\n").append(items.size()).append(' ').append(label);
        items.stream().limit(MAX_ITEMS_IN_MESSAGE).forEach(item -> sb.append("\n• ").append(line.apply(item)));
        if (items.size() > MAX_ITEMS_IN_MESSAGE) {
            sb.append("\n• … e mais ").append(items.size() - MAX_ITEMS_IN_MESSAGE).append(" (ver resumo completo)");
        }
    }

    private List<OverdueLoan> overdueLoans(LocalDate today) {
        return loanRepository.findOpenOverdue(today).stream()
                .filter(l -> l.getStatus() == LoanStatus.ATIVO || l.getStatus() == LoanStatus.ATRASADO)
                .map(l -> new OverdueLoan(l.getId(), l.getBeneficiary().getId(), l.getBeneficiary().getFullName(),
                        patrimony(l.getEquipment()), l.getEquipment() != null ? l.getEquipment().getType() : null,
                        l.getExpectedReturnDate(), l.daysOverdue(today)))
                .sorted(Comparator.comparingLong(OverdueLoan::daysOverdue).reversed())
                .toList();
    }

    /** Reservas expiradas pela rotina (RN33) nas últimas 24 h, a partir da trilha de auditoria automática. */
    private List<ExpiredReservation> expiredReservations(LocalDateTime now) {
        List<AuditLog> logs = auditLogRepository.search(WaitlistService.ENTITY_NAME, null, now.minusHours(24), now);
        return logs.stream()
                .filter(AuditLog::isAutomatic)
                .filter(log -> WaitlistService.RULE_EXPIRATION.equals(log.getOriginRule()))
                .filter(log -> log.getDetails() != null && log.getDetails().contains(WaitlistService.EVENT_EXPIRED))
                .map(log -> toExpired(log).orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ExpiredReservation::expiredAt).reversed())
                .toList();
    }

    private Optional<ExpiredReservation> toExpired(AuditLog log) {
        Long entryId;
        try {
            entryId = Long.valueOf(log.getEntityId());
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        return waitlistRepository.findById(entryId).map((WaitlistEntry e) -> new ExpiredReservation(e.getId(),
                e.getBeneficiary().getId(), e.getBeneficiary().getFullName(), e.getEquipmentType(),
                log.getOccurredAt(), e.getStatus() == WaitlistStatus.REMOVIDO));
    }

    private List<OpenMaintenance> openMaintenances(LocalDate today) {
        return maintenanceRepository.findByStatus(MaintenanceStatus.EM_ANDAMENTO).stream()
                .map((Maintenance m) -> new OpenMaintenance(m.getId(),
                        m.getEquipment() != null ? m.getEquipment().getId() : null, patrimony(m.getEquipment()),
                        m.getEquipment() != null ? m.getEquipment().getType() : null, m.getType(), m.getEntryDate(),
                        m.getEntryDate() != null ? ChronoUnit.DAYS.between(m.getEntryDate(), today) : 0,
                        m.getTechnicalResponsible()))
                .sorted(Comparator.comparingLong(OpenMaintenance::daysOpen).reversed())
                .toList();
    }

    private static String patrimony(Equipment equipment) {
        return equipment != null ? equipment.getPatrimonyNumber() : null;
    }
}
