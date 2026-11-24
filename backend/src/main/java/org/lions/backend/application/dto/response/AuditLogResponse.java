package org.lions.backend.application.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import org.lions.backend.domain.enums.AuditAction;

import java.time.LocalDateTime;

/**
 * Registro da trilha de auditoria (RN25/RN52/RN58). {@code details} é JSON: em alterações,
 * {@code {campo: {"old": valorAnterior, "new": valorNovo}}}.
 */
public record AuditLogResponse(
        Long id,
        String entityName,
        String entityId,
        AuditAction action,
        String actionDescription,
        String username,
        boolean automatic,
        String originRule,
        JsonNode details,
        LocalDateTime occurredAt) {
}
