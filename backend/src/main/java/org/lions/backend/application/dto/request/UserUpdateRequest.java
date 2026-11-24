package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Edição de nome/e-mail (auditada — RN58). Perfil, status e senha têm endpoints próprios. */
public record UserUpdateRequest(
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String fullName,
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 100, message = "O e-mail deve ter no máximo 100 caracteres.")
        String email) {
}
