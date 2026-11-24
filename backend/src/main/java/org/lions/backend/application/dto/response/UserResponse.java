package org.lions.backend.application.dto.response;

import org.lions.backend.domain.entity.User;
import org.lions.backend.domain.enums.UserRole;

import java.time.LocalDateTime;

/** Usuário do sistema (sem senha). */
public record UserResponse(
        Long id,
        String username,
        String fullName,
        String email,
        UserRole role,
        String roleDescription,
        boolean active,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(), user.getRole(),
                user.getRole() != null ? user.getRole().getDescription() : null, user.isActive(),
                user.getLastLoginAt(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
