package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ParameterUpdateRequest(
        @NotBlank(message = "O valor do parâmetro é obrigatório.")
        String value) {
}
