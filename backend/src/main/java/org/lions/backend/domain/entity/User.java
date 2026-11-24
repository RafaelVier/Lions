package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lions.backend.domain.enums.UserRole;
import org.lions.backend.domain.exception.BusinessException;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String password;
    private UserRole role;
    private boolean active;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    /** RN02 — o usuário não pode alterar o próprio perfil de acesso. */
    public void changeRole(UserRole newRole, String actingUsername) {
        if (newRole == null) {
            throw new BusinessException("Perfil de acesso é obrigatório.");
        }
        if (actingUsername != null && actingUsername.equalsIgnoreCase(username)) {
            throw new BusinessException("Não é permitido alterar o próprio perfil de acesso.");
        }
        this.role = newRole;
    }

    public void registerLogin(LocalDateTime now) {
        this.lastLoginAt = now;
    }

    public boolean isAdmin() {
        return role == UserRole.ROLE_ADMIN;
    }
}
