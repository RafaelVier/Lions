package org.lions.backend.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.lions.backend.domain.entity.SyncOperation;
import org.lions.backend.domain.enums.SyncOperationType;
import org.lions.backend.domain.enums.SyncResultStatus;
import org.lions.backend.domain.repository.SyncOperationRepositoryPort;
import org.lions.backend.infrastructure.persistence.entity.SyncOperationJpaEntity;
import org.lions.backend.infrastructure.persistence.repository.SyncOperationJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SyncOperationRepositoryAdapter implements SyncOperationRepositoryPort {

    private final SyncOperationJpaRepository jpaRepository;

    @Override
    @Transactional
    public SyncOperation save(SyncOperation o) {
        return toDomain(jpaRepository.save(SyncOperationJpaEntity.builder()
                .id(o.getId())
                .clientOperationId(o.getClientOperationId())
                .type(o.getType())
                .beneficiaryId(o.getBeneficiaryId())
                .equipmentId(o.getEquipmentId())
                .loanId(o.getLoanId())
                .occurredAt(o.getOccurredAt())
                .receivedAt(o.getReceivedAt())
                .username(o.getUsername())
                .result(o.getResult())
                .message(o.getMessage())
                .resultingLoanId(o.getResultingLoanId())
                .conflictId(o.getConflictId())
                .build()));
    }

    @Override
    public Optional<SyncOperation> findByClientOperationId(String clientOperationId) {
        return jpaRepository.findByClientOperationId(clientOperationId).map(this::toDomain);
    }

    @Override
    public Optional<SyncOperation> findApplied(Long loanId, SyncOperationType type) {
        return jpaRepository.findFirstByResultingLoanIdAndResultAndType(loanId, SyncResultStatus.APLICADA, type)
                .map(this::toDomain);
    }

    private SyncOperation toDomain(SyncOperationJpaEntity e) {
        return SyncOperation.builder()
                .id(e.getId())
                .clientOperationId(e.getClientOperationId())
                .type(e.getType())
                .beneficiaryId(e.getBeneficiaryId())
                .equipmentId(e.getEquipmentId())
                .loanId(e.getLoanId())
                .occurredAt(e.getOccurredAt())
                .receivedAt(e.getReceivedAt())
                .username(e.getUsername())
                .result(e.getResult())
                .message(e.getMessage())
                .resultingLoanId(e.getResultingLoanId())
                .conflictId(e.getConflictId())
                .build();
    }
}
