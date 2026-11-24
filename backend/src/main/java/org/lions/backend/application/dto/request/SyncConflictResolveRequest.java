package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Conferência manual de conflito de sincronização (RN60) — somente Administrador. */
public record SyncConflictResolveRequest(
        @NotBlank(message = "Descreva como o conflito foi resolvido.")
        @Size(max = 1000, message = "A nota deve ter no máximo 1000 caracteres.")
        String note) {
}
