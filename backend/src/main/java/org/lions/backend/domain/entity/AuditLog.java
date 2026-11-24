package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.lions.backend.domain.enums.AuditAction;

import java.time.LocalDateTime;

/** Trilha de auditoria imutável (RN25/RN52/RN58). {@code details} é JSON. */
@Getter
@Builder
@AllArgsConstructor
public class AuditLog {

    private final Long id;
    private final String entityName;
    private final String entityId;
    private final AuditAction action;
    private final String username;
    private final boolean automatic;
    private final String originRule;
    private final String details;
    private final LocalDateTime occurredAt;
}
