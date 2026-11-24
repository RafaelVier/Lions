package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.lions.backend.domain.enums.WriteOffReason;

/** Baixa de equipamento (RN13) — somente Administrador. */
public record WriteOffRequest(
        @NotNull(message = "O motivo da baixa é obrigatório.")
        WriteOffReason reason,
        @Size(max = 500, message = "As observações devem ter no máximo 500 caracteres.")
        String notes) {
}
