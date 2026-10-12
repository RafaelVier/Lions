package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lions.backend.domain.enums.ReturnCondition;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanReturnRequest {

    @NotNull(message = "A condição de retorno do equipamento é obrigatória.")
    private ReturnCondition returnCondition; // BOM_ESTADO, PRECISA_MANUTENCAO, DANIFICADO

    private String damageDescription; // "Ex: apoio de pé quebrado"
    private String returnPhotoUrl;
    private boolean responsibilityAssigned;
}
