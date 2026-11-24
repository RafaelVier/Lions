package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.lions.backend.domain.enums.CostType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Custo (peça/serviço) de uma manutenção (RN38). Imutável: correções são feitas por lançamento de ajuste. */
@Getter
@Builder
@AllArgsConstructor
public class MaintenanceCost {

    private final Long id;
    private final Long maintenanceId;
    private final CostType type;
    private final String description;
    private final String partReplaced;
    private final BigDecimal amount;
    private final boolean adjustment;
    private final String adjustmentReason;
    private final String registeredBy;
    private final LocalDateTime registeredAt;
}
