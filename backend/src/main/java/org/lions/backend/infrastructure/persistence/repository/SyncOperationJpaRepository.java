package org.lions.backend.infrastructure.persistence.repository;

import org.lions.backend.domain.enums.SyncOperationType;
import org.lions.backend.domain.enums.SyncResultStatus;
import org.lions.backend.infrastructure.persistence.entity.SyncOperationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SyncOperationJpaRepository extends JpaRepository<SyncOperationJpaEntity, Long> {
    Optional<SyncOperationJpaEntity> findByClientOperationId(String clientOperationId);

    Optional<SyncOperationJpaEntity> findFirstByResultingLoanIdAndResultAndType(Long loanId, SyncResultStatus result,
                                                                                SyncOperationType type);
}
