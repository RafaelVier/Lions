package org.lions.backend.application.service.loan;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lions.backend.application.event.BeneficiaryRestrictedEvent;
import org.lions.backend.application.service.audit.AuditService;
import org.lions.backend.application.service.notification.NotificationService;
import org.lions.backend.application.service.parameter.ParameterService;
import org.lions.backend.application.support.AuditDetails;
import org.lions.backend.domain.entity.Loan;
import org.lions.backend.domain.entity.Occurrence;
import org.lions.backend.domain.enums.AuditAction;
import org.lions.backend.domain.enums.LoanStatus;
import org.lions.backend.domain.enums.NotificationType;
import org.lions.backend.domain.enums.OccurrenceStatus;
import org.lions.backend.domain.enums.OccurrenceType;
import org.lions.backend.domain.repository.LoanRepositoryPort;
import org.lions.backend.domain.repository.OccurrenceRepositoryPort;
import org.lions.backend.application.port.CurrentUserPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Rotina diária de atraso (RN27): ATIVO → ATRASADO a partir do dia seguinte ao vencimento; com
 * {@code loan.default-escalation-days} dias de atraso registra ocorrência de INADIMPLENCIA e avisa o Admin.
 * Beneficiário em atraso/inadimplente é removido da fila (RN34). Ações automáticas auditadas (RN52).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoanOverdueService {

    public static final String RULE = "RN27";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LoanRepositoryPort loanRepository;
    private final OccurrenceRepositoryPort occurrenceRepository;
    private final NotificationService notificationService;
    private final ParameterService parameterService;
    private final AuditService auditService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public record Result(int markedOverdue, int defaultsRegistered) {
    }

    @Transactional
    public Result processOverdueLoans() {
        LocalDate today = LocalDate.now(clock);
        int escalationDays = parameterService.defaultEscalationDays();
        int marked = 0;
        int defaults = 0;
        for (Loan loan : loanRepository.findOpenOverdue(today)) {
            Long beneficiaryId = loan.getBeneficiary().getId();
            if (loan.markOverdue(today)) {
                loan = loanRepository.save(loan);
                auditService.recordAutomatic(LoanService.ENTITY_NAME, loan.getId(), AuditAction.STATUS_CHANGE, RULE,
                        AuditDetails.of("status", AuditDetails.oldNew(LoanStatus.ATIVO, LoanStatus.ATRASADO),
                                "expectedReturnDate", loan.getExpectedReturnDate()));
                events.publishEvent(new BeneficiaryRestrictedEvent(beneficiaryId, "Empréstimo em atraso."));
                marked++;
            }
            if (loan.getStatus() == LoanStatus.ATRASADO
                    && loan.daysOverdue(today) >= escalationDays
                    && !occurrenceRepository.existsByLoanIdAndType(loan.getId(), OccurrenceType.INADIMPLENCIA)) {
                registerDefault(loan, today);
                defaults++;
            }
        }
        if (marked > 0 || defaults > 0) {
            log.info("Rotina de atraso: {} empréstimo(s) marcados como atrasados, {} inadimplência(s) registradas.",
                    marked, defaults);
        }
        return new Result(marked, defaults);
    }

    private void registerDefault(Loan loan, LocalDate today) {
        long days = loan.daysOverdue(today);
        String patrimony = loan.getEquipment() != null ? loan.getEquipment().getPatrimonyNumber() : "-";
        Occurrence occurrence = occurrenceRepository.save(Occurrence.builder()
                .beneficiary(loan.getBeneficiary())
                .loanId(loan.getId())
                .type(OccurrenceType.INADIMPLENCIA)
                .status(OccurrenceStatus.ABERTA)
                .description("Inadimplência: empréstimo #" + loan.getId() + " (" + patrimony + ") com " + days
                        + " dias de atraso (vencimento em " + DATE.format(loan.getExpectedReturnDate()) + ").")
                .automatic(true)
                .createdBy(CurrentUserPort.SYSTEM_USERNAME)
                .build());
        auditService.recordAutomatic(OccurrenceService.ENTITY_NAME, occurrence.getId(), AuditAction.CREATE, RULE,
                AuditDetails.of("type", OccurrenceType.INADIMPLENCIA, "loanId", loan.getId(), "daysOverdue", days));
        notificationService.notifyAdmins(NotificationType.INADIMPLENCIA,
                "Inadimplência registrada: " + loan.getBeneficiary().getFullName() + " — empréstimo #" + loan.getId()
                        + " (" + patrimony + ") com " + days + " dias de atraso.");
        events.publishEvent(new BeneficiaryRestrictedEvent(loan.getBeneficiary().getId(),
                "Inadimplência registrada."));
    }
}
