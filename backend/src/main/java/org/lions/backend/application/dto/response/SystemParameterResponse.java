package org.lions.backend.application.dto.response;

import java.time.LocalDateTime;

/**
 * @param value        valor efetivo (armazenado ou default)
 * @param defaultValue valor padrão do sistema
 * @param customized   {@code true} se há valor armazenado diferente do default do sistema
 */
public record SystemParameterResponse(
        String key,
        String value,
        String defaultValue,
        String description,
        boolean customized,
        String updatedBy,
        LocalDateTime updatedAt) {
}
