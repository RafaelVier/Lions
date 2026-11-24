package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Lançamento de ajuste em registro concluído (RN41) — somente Administrador. */
public record AdjustmentRequest(
        @NotBlank(message = "A descrição do ajuste é obrigatória.")
        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres.")
        String description,
        @NotBlank(message = "O motivo do ajuste é obrigatório.")
        @Size(max = 500, message = "O motivo deve ter no máximo 500 caracteres.")
        String reason) {
}
