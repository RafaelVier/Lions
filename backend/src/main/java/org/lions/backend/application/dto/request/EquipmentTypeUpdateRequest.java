package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Atualização do tipo de equipamento (RN18).
 *
 * @param loanDays prazo do empréstimo em dias; {@code null} volta a usar o prazo padrão ({@code loan.default-days})
 */
public record EquipmentTypeUpdateRequest(
        @NotBlank(message = "A descrição é obrigatória.")
        @Size(max = 80, message = "A descrição deve ter no máximo 80 caracteres.")
        String description,
        @Positive(message = "O prazo deve ser um número positivo de dias.")
        @Max(value = 3650, message = "O prazo deve ser de no máximo 3650 dias.")
        Integer loanDays) {
}
