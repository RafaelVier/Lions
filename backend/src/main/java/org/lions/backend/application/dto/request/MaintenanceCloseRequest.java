package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.lions.backend.domain.enums.MaintenanceVerdict;

/** Encerramento com parecer (RN37): APTO → DISPONIVEL; INSERVIVEL → baixa (somente Admin). */
public record MaintenanceCloseRequest(
        @NotNull(message = "O parecer é obrigatório.")
        MaintenanceVerdict verdict,
        @Size(max = 1000, message = "As observações devem ter no máximo 1000 caracteres.")
        String notes) {
}
