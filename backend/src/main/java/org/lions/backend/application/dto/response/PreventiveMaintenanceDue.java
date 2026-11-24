package org.lions.backend.application.dto.response;

import org.lions.backend.domain.enums.EquipmentType;

import java.time.LocalDate;

/** Equipamento que atingiu o critério de manutenção preventiva (RN39). */
public record PreventiveMaintenanceDue(
        Long equipmentId,
        String patrimonyNumber,
        EquipmentType type,
        LocalDate lastMaintenanceDate,
        int loanCycles,
        String reason) {
}
