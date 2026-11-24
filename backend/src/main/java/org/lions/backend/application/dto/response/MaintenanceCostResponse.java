package org.lions.backend.application.dto.response;

import org.lions.backend.domain.entity.MaintenanceCost;
import org.lions.backend.domain.enums.CostType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MaintenanceCostResponse(
        Long id,
        CostType type,
        String typeDescription,
        String description,
        String partReplaced,
        BigDecimal amount,
        boolean adjustment,
        String adjustmentReason,
        String registeredBy,
        LocalDateTime registeredAt) {

    public static MaintenanceCostResponse from(MaintenanceCost cost) {
        return new MaintenanceCostResponse(
                cost.getId(),
                cost.getType(),
                cost.getType() != null ? cost.getType().getDescription() : null,
                cost.getDescription(),
                cost.getPartReplaced(),
                cost.getAmount(),
                cost.isAdjustment(),
                cost.getAdjustmentReason(),
                cost.getRegisteredBy(),
                cost.getRegisteredAt());
    }
}
