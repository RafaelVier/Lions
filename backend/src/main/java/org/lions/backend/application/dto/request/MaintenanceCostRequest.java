package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.lions.backend.domain.enums.CostType;

import java.math.BigDecimal;

/**
 * Custo de manutenção (RN38). Custo comum: só em manutenção em andamento e valor ≥ 0. Ajuste ({@code adjustment=true},
 * RN41): somente Admin, com motivo, em qualquer status e pode ser negativo.
 */
public record MaintenanceCostRequest(
        @NotNull(message = "O tipo do custo é obrigatório.")
        CostType type,
        @NotBlank(message = "A descrição do custo é obrigatória.")
        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
        String description,
        @Size(max = 150, message = "A peça substituída deve ter no máximo 150 caracteres.")
        String partReplaced,
        @NotNull(message = "O valor é obrigatório.")
        @Digits(integer = 10, fraction = 2, message = "Valor inválido (até 2 casas decimais).")
        BigDecimal amount,
        Boolean adjustment,
        @Size(max = 500, message = "O motivo do ajuste deve ter no máximo 500 caracteres.")
        String adjustmentReason) {

    public boolean isAdjustment() {
        return Boolean.TRUE.equals(adjustment);
    }
}
