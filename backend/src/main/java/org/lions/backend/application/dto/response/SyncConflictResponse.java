package org.lions.backend.application.dto.response;

import org.lions.backend.domain.entity.SyncConflict;
import org.lions.backend.domain.enums.SyncConflictStatus;
import org.lions.backend.domain.enums.SyncOperationType;
import org.lions.backend.domain.enums.SyncPrevailing;

import java.time.LocalDateTime;

public record SyncConflictResponse(
        Long id,
        Long equipmentId,
        Long existingLoanId,
        String clientOperationId,
        SyncOperationType operationType,
        LocalDateTime incomingOccurredAt,
        LocalDateTime existingOccurredAt,
        SyncPrevailing prevailing,
        String prevailingDescription,
        String description,
        SyncConflictStatus status,
        String username,
        String resolutionNote,
        String resolvedBy,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt) {

    public static SyncConflictResponse from(SyncConflict c) {
        return new SyncConflictResponse(c.getId(), c.getEquipmentId(), c.getExistingLoanId(), c.getClientOperationId(),
                c.getOperationType(), c.getIncomingOccurredAt(), c.getExistingOccurredAt(), c.getPrevailing(),
                c.getPrevailing() != null ? c.getPrevailing().getDescription() : null, c.getDescription(),
                c.getStatus(), c.getUsername(), c.getResolutionNote(), c.getResolvedBy(), c.getResolvedAt(),
                c.getCreatedAt());
    }
}
