package org.lions.backend.application.dto.response;

import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.MaintenanceType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Resumo diário do Administrador (RN49): empréstimos em atraso, reservas expiradas nas últimas 24 h, manutenções em
 * aberto e revisões preventivas vencidas. Contém dados pessoais — somente perfil Admin.
 */
public record DailySummaryResponse(
        LocalDate date,
        LocalDateTime generatedAt,
        List<OverdueLoan> overdueLoans,
        List<ExpiredReservation> expiredReservations,
        List<OpenMaintenance> openMaintenances,
        List<PreventiveMaintenanceDue> preventiveDue) {

    public record OverdueLoan(Long loanId, Long beneficiaryId, String beneficiaryName, String patrimonyNumber,
                              EquipmentType equipmentType, LocalDate expectedReturnDate, long daysOverdue) {
    }

    /**
     * @param removedFromWaitlist {@code true} se o beneficiário saiu da fila por expirações consecutivas (RN33)
     */
    public record ExpiredReservation(Long waitlistEntryId, Long beneficiaryId, String beneficiaryName,
                                     EquipmentType equipmentType, LocalDateTime expiredAt,
                                     boolean removedFromWaitlist) {
    }

    public record OpenMaintenance(Long maintenanceId, Long equipmentId, String patrimonyNumber,
                                  EquipmentType equipmentType, MaintenanceType type, LocalDate entryDate,
                                  long daysOpen, String technicalResponsible) {
    }

    public boolean isEmpty() {
        return overdueLoans.isEmpty() && expiredReservations.isEmpty() && openMaintenances.isEmpty()
                && preventiveDue.isEmpty();
    }
}
