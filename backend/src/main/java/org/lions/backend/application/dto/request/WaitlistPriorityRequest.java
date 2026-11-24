package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.lions.backend.domain.enums.PriorityReason;

/** Prioridade na fila (RN30) — somente Administrador, com justificativa. */
public record WaitlistPriorityRequest(
        @NotNull(message = "O motivo da prioridade é obrigatório.")
        PriorityReason reason,
        @NotBlank(message = "A justificativa da prioridade é obrigatória.")
        @Size(max = 500, message = "A justificativa deve ter no máximo 500 caracteres.")
        String justification) {
}
