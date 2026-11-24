package org.lions.backend.domain.repository;

import org.lions.backend.domain.entity.SyncOperation;
import org.lions.backend.domain.enums.SyncOperationType;

import java.util.Optional;

public interface SyncOperationRepositoryPort {
    SyncOperation save(SyncOperation operation);

    Optional<SyncOperation> findByClientOperationId(String clientOperationId);

    /** Operação aplicada do tipo sobre o empréstimo (para saber quando foi feita no dispositivo). */
    Optional<SyncOperation> findApplied(Long loanId, SyncOperationType type);
}
