package org.lions.backend.application.dto.response;

import java.time.Instant;

/**
 * Sessão autenticada (RN57): token JWT (Bearer) válido por {@code expiresIn} segundos (30 min). Renove com
 * {@code POST /auth/refresh} enquanto houver atividade; sem renovação a sessão expira por inatividade.
 */
public record AuthResponse(
        String token,
        String type,
        long expiresIn,
        Instant expiresAt,
        UserResponse user) {

    public static AuthResponse bearer(String token, long expiresInSeconds, Instant expiresAt, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresInSeconds, expiresAt, user);
    }
}
