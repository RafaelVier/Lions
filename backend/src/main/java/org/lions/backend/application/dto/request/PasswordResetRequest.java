package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Redefinição de senha de outro usuário pelo Admin (senha forte — RN57). */
public record PasswordResetRequest(
        @NotBlank(message = "A nova senha é obrigatória.")
        @Size(max = 72, message = "A senha deve ter no máximo 72 caracteres.")
        String newPassword) {
}
