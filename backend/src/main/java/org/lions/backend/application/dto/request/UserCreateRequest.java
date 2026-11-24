package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.lions.backend.domain.enums.UserRole;

/** Criação de usuário pelo Admin (RN02). Senha forte (RN57): ≥ 8, maiúscula, minúscula, número e especial. */
public record UserCreateRequest(
        @NotBlank(message = "O usuário é obrigatório.")
        @Pattern(regexp = "[A-Za-z0-9._-]{3,50}",
                message = "O usuário deve ter de 3 a 50 caracteres (letras, números, ponto, hífen ou sublinhado).")
        String username,
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String fullName,
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 100, message = "O e-mail deve ter no máximo 100 caracteres.")
        String email,
        @NotBlank(message = "A senha é obrigatória.")
        @Size(max = 72, message = "A senha deve ter no máximo 72 caracteres.")
        String password,
        @NotNull(message = "O perfil é obrigatório.")
        UserRole role) {
}
