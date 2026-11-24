package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Resolução de ocorrência (RN20) — somente Administrador. */
public record OccurrenceResolveRequest(
        @NotBlank(message = "A nota de resolução é obrigatória.")
        @Size(max = 1000, message = "A nota deve ter no máximo 1000 caracteres.")
        String note) {
}
