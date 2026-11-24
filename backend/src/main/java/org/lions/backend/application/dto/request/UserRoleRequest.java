package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.NotNull;
import org.lions.backend.domain.enums.UserRole;

/** Alteração de perfil (RN02): o Admin não pode alterar o próprio perfil. */
public record UserRoleRequest(
        @NotNull(message = "O perfil é obrigatório.")
        UserRole role) {
}
