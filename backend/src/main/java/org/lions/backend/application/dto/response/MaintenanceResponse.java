package org.lions.backend.application.dto.response;

import org.lions.backend.domain.entity.Equipment;
import org.lions.backend.domain.entity.Maintenance;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.MaintenanceStatus;
import org.lions.backend.domain.enums.MaintenanceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Manutenção com custos e total (RN38). Encerrada é imutável; correções por ajuste (RN41). */
public record MaintenanceResponse(
        Long id,
        Long equipmentId,
        String patrimonyNumber,
        EquipmentType equipmentType,
        String equipmentTypeDescription,
        MaintenanceType type,
        String typeDescription,
        MaintenanceStatus status,
        String statusDescription,
        LocalDate entryDate,
        LocalDate exitDate,
        String defectDescription,
        String technicalResponsible,
        String verdictNotes,
        Long originLoanId,
        String openedBy,
        String closedBy,
        List<MaintenanceCostResponse> costs,
        BigDecimal totalCost,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static MaintenanceResponse from(Maintenance m) {
        Equipment e = m.getEquipment();
        return new MaintenanceResponse(
                m.getId(),
                e != null ? e.getId() : null,
                e != null ? e.getPatrimonyNumber() : null,
                e != null ? e.getType() : null,
                e != null && e.getType() != null ? e.getType().getDescription() : null,
                m.getType(),
                m.getType() != null ? m.getType().getDescription() : null,
                m.getStatus(),
                m.getStatus() != null ? m.getStatus().getDescription() : null,
                m.getEntryDate(),
                m.getExitDate(),
                m.getDefectDescription(),
                m.getTechnicalResponsible(),
                m.getVerdictNotes(),
                m.getOriginLoanId(),
                m.getOpenedBy(),
                m.getClosedBy(),
                m.getCosts() == null ? List.of() : m.getCosts().stream().map(MaintenanceCostResponse::from).toList(),
                m.totalCost(),
                m.getCreatedAt(),
                m.getUpdatedAt());
    }
}
