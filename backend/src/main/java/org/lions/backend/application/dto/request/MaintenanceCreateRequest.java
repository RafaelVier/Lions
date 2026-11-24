package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.lions.backend.domain.enums.MaintenanceType;

import java.time.LocalDate;

/**
 * Abertura de manutenção (RN36): defeito, data de entrada e responsável técnico. {@code type} nulo = CORRETIVA;
 * {@code entryDate} nula = hoje. O equipamento passa a EM_MANUTENCAO automaticamente.
 */
public record MaintenanceCreateRequest(
        @NotNull(message = "O equipamento é obrigatório.")
        Long equipmentId,
        MaintenanceType type,
        @NotBlank(message = "A descrição do defeito é obrigatória.")
        @Size(max = 1000, message = "A descrição do defeito deve ter no máximo 1000 caracteres.")
        String defectDescription,
        @PastOrPresent(message = "A data de entrada não pode ser futura.")
        LocalDate entryDate,
        @NotBlank(message = "O responsável técnico é obrigatório.")
        @Size(max = 150, message = "O responsável técnico deve ter no máximo 150 caracteres.")
        String technicalResponsible) {
}
