package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo genérico com motivo obrigatório (rejeição, cancelamento, inativação, remoção da fila). */
public record ReasonRequest(
        @NotBlank(message = "O motivo é obrigatório.")
        @Size(max = 500, message = "O motivo deve ter no máximo 500 caracteres.")
        String reason) {
}
