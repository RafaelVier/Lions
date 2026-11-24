package org.lions.backend.application.service.maintenance;

import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.response.PreventiveMaintenanceDue;
import org.lions.backend.application.service.notification.NotificationService;
import org.lions.backend.application.service.parameter.ParameterService;
import org.lions.backend.domain.entity.Equipment;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.NotificationType;
import org.lions.backend.domain.repository.EquipmentRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Contrato §6.2 — dono: Equipamentos + Manutenção (Frente C). */
@Service
@RequiredArgsConstructor
public class PreventiveMaintenanceService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EquipmentRepositoryPort equipmentRepository;
    private final ParameterService parameterService;
    private final NotificationService notificationService;
    private final Clock clock;

    /**
     * Equipamentos ativos (fora de manutenção) que atingiram {@code maintenance.preventive-months} meses sem
     * manutenção ou {@code maintenance.preventive-loan-cycles} ciclos de empréstimo (RN39). Sem manutenção registrada,
     * conta a partir do cadastro.
     */
    @Transactional(readOnly = true)
    public List<PreventiveMaintenanceDue> findDue() {
        LocalDate today = LocalDate.now(clock);
        int months = parameterService.preventiveMonths();
        int cycles = parameterService.preventiveLoanCycles();
        List<PreventiveMaintenanceDue> due = new ArrayList<>();
        for (Equipment equipment : equipmentRepository.findAllActive()) {
            if (equipment.getStatus() == EquipmentStatus.EM_MANUTENCAO) {
                continue;
            }
            LocalDate reference = equipment.getLastMaintenanceDate() != null
                    ? equipment.getLastMaintenanceDate()
                    : (equipment.getCreatedAt() != null ? equipment.getCreatedAt().toLocalDate() : null);
            List<String> reasons = new ArrayList<>();
            if (reference != null && !reference.plusMonths(months).isAfter(today)) {
                reasons.add(months + " meses ou mais sem manutenção (desde " + DATE.format(reference) + ")");
            }
            if (equipment.getLoanCyclesSinceMaintenance() >= cycles) {
                reasons.add(equipment.getLoanCyclesSinceMaintenance() + " ciclos de empréstimo desde a última "
                        + "manutenção (limite " + cycles + ")");
            }
            if (!reasons.isEmpty()) {
                due.add(new PreventiveMaintenanceDue(equipment.getId(), equipment.getPatrimonyNumber(),
                        equipment.getType(), equipment.getLastMaintenanceDate(),
                        equipment.getLoanCyclesSinceMaintenance(), String.join("; ", reasons) + "."));
            }
        }
        due.sort(Comparator.comparing(PreventiveMaintenanceDue::patrimonyNumber));
        return due;
    }

    /** RN39 — avisa o Admin uma vez por item/mês. Retorna quantos avisos novos foram criados. */
    @Transactional
    public int notifyDue() {
        LocalDate monthReference = LocalDate.now(clock).withDayOfMonth(1);
        int created = 0;
        for (PreventiveMaintenanceDue item : findDue()) {
            boolean notified = notificationService.notifyAdmins(NotificationType.PREVENTIVA_VENCIDA,
                    "Revisão preventiva pendente: " + item.patrimonyNumber() + " (" + item.type().getDescription()
                            + ") — " + item.reason(),
                    "EQUIPMENT:" + item.equipmentId(), monthReference);
            if (notified) {
                created++;
            }
        }
        return created;
    }
}
