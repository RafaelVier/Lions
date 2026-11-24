package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.lions.backend.domain.enums.ReturnCondition;

import java.time.LocalDate;

/**
 * Devolução com vistoria (RN24/RN40). {@code returnDate} nula = hoje. {@code PRECISA_MANUTENCAO}/{@code DANIFICADO}
 * enviam o item para manutenção corretiva automática e geram ocorrência de dano vinculada ao beneficiário (RN40);
 * {@code DANIFICADO} exige descrição.
 */
public record LoanReturnRequest(
        @NotNull(message = "A condição do equipamento na devolução é obrigatória.")
        ReturnCondition returnCondition,
        @Size(max = 1000, message = "A descrição da avaria deve ter no máximo 1000 caracteres.")
        String damageDescription,
        @Size(max = 500, message = "A URL da foto deve ter no máximo 500 caracteres.")
        String returnPhotoUrl,
        @PastOrPresent(message = "A data da devolução não pode ser futura.")
        LocalDate returnDate) {
}
