package org.lions.backend.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.lions.backend.domain.entity.SyncConflict;
import org.lions.backend.domain.enums.SyncConflictStatus;
import org.lions.backend.domain.repository.SyncConflictRepositoryPort;
import org.lions.backend.infrastructure.persistence.entity.SyncConflictJpaEntity;
import org.lions.backend.infrastructure.persistence.repository.SyncConflictJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SyncConflictRepositoryAdapter implements SyncConflictRepositoryPort {

    private final SyncConflictJpaRepository jpaRepository;

    @Override
    @Transactional
    public SyncConflict save(SyncConflict c) {
        return toDomain(jpaRepository.save(SyncConflictJpaEntity.builder()
                .id(c.getId())
                .equipmentId(c.getEquipmentId())
                .existingLoanId(c.getExistingLoanId())
                .clientOperationId(c.getClientOperationId())
                .operationType(c.getOperationType())
                .incomingOccurredAt(c.getIncomingOccurredAt())
                .existingOccurredAt(c.getExistingOccurredAt())
                .prevailing(c.getPrevailing())
                .description(c.getDescription())
                .status(c.getStatus())
                .username(c.getUsername())
                .resolutionNote(c.getResolutionNote())
                .resolvedBy(c.getResolvedBy())
                .resolvedAt(c.getResolvedAt())
                .createdAt(c.getCreatedAt())
                .build()));
    }

    @Override
    public Optional<SyncConflict> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<SyncConflict> findAll(SyncConflictStatus status) {
        return (status == null ? jpaRepository.findAllByOrderByIdDesc()
                : jpaRepository.findByStatusOrderByIdDesc(status)).stream().map(this::toDomain).toList();
    }

    private SyncConflict toDomain(SyncConflictJpaEntity e) {
        return SyncConflict.builder()
                .id(e.getId())
                .equipmentId(e.getEquipmentId())
                .existingLoanId(e.getExistingLoanId())
                .clientOperationId(e.getClientOperationId())
                .operationType(e.getOperationType())
                .incomingOccurredAt(e.getIncomingOccurredAt())
                .existingOccurredAt(e.getExistingOccurredAt())
                .prevailing(e.getPrevailing())
                .description(e.getDescription())
                .status(e.getStatus())
                .username(e.getUsername())
                .resolutionNote(e.getResolutionNote())
                .resolvedBy(e.getResolvedBy())
                .resolvedAt(e.getResolvedAt())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
