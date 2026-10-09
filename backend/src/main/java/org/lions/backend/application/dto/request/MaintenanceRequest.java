package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lions.backend.domain.enums.EquipmentCondition;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRequest {

    @NotNull(message = "O ID do equipamento é obrigatório.")
    private Long equipmentId;

    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private BigDecimal cost;
    private EquipmentCondition conditionAfterFix; // Quando concluída
}
