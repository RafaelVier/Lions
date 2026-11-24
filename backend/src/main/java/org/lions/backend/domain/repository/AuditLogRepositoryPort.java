package org.lions.backend.domain.repository;

import org.lions.backend.domain.entity.AuditLog;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepositoryPort {
    AuditLog save(AuditLog auditLog);

    /** Filtros opcionais ({@code null} = sem filtro), ordenado por {@code occurredAt desc}. */
    List<AuditLog> search(String entityName, String entityId, LocalDateTime from, LocalDateTime to);
}
