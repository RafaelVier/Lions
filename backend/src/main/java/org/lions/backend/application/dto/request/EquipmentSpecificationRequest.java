package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Especificação do equipamento (nome/valor). */
public record EquipmentSpecificationRequest(
        @NotBlank(message = "O nome da especificação é obrigatório.")
        @Size(max = 80, message = "O nome da especificação deve ter no máximo 80 caracteres.")
        String name,
        @NotBlank(message = "O valor da especificação é obrigatório.")
        @Size(max = 255, message = "O valor da especificação deve ter no máximo 255 caracteres.")
        String value) {
}
