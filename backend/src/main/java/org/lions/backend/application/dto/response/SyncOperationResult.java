package org.lions.backend.application.dto.response;

import org.lions.backend.domain.enums.SyncOperationType;
import org.lions.backend.domain.enums.SyncResultStatus;

/**
 * Resultado de uma operação do lote: {@code APLICADA} (com {@code loanId}), {@code RECUSADA} (regra de negócio —
 * ver {@code message}), {@code CONFLITO} (ver {@code conflictId}) ou {@code DUPLICADA} (já processada antes; traz o
 * resultado original em {@code message}).
 */
public record SyncOperationResult(
        String clientOperationId,
        SyncOperationType type,
        SyncResultStatus status,
        String message,
        Long loanId,
        Long conflictId) {
}
