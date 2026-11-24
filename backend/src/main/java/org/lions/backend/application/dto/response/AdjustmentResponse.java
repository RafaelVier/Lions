package org.lions.backend.application.dto.response;

import java.time.LocalDateTime;

/** Lançamento de ajuste (RN41): correção de registro concluído com autor e motivo, mantido na auditoria. */
public record AdjustmentResponse(
        Long id,
        String description,
        String reason,
        String registeredBy,
        LocalDateTime registeredAt) {
}
