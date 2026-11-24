package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Troca da própria senha (RN57): exige a senha atual e uma nova senha forte. */
public record PasswordChangeRequest(
        @NotBlank(message = "A senha atual é obrigatória.")
        String currentPassword,
        @NotBlank(message = "A nova senha é obrigatória.")
        @Size(max = 72, message = "A senha deve ter no máximo 72 caracteres.")
        String newPassword) {
}
