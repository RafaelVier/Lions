package org.lions.backend.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.lions.backend.domain.entity.LoanOrder;
import org.lions.backend.domain.enums.OrderStatus;
import org.lions.backend.domain.repository.LoanOrderRepositoryPort;
import org.lions.backend.infrastructure.persistence.entity.LoanOrderJpaEntity;
import org.lions.backend.infrastructure.persistence.repository.BeneficiaryJpaRepository;
import org.lions.backend.infrastructure.persistence.repository.LoanOrderJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanOrderRepositoryAdapter implements LoanOrderRepositoryPort {

    private final LoanOrderJpaRepository jpaRepository;
    private final BeneficiaryJpaRepository beneficiaryJpaRepository;
    private final BeneficiaryRepositoryAdapter beneficiaryAdapter;

    @Override
    @Transactional
    public LoanOrder save(LoanOrder order) {
        return toDomainEntity(jpaRepository.save(toJpaEntity(order)));
    }

    @Override
    public Optional<LoanOrder> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomainEntity);
    }

    @Override
    public List<LoanOrder> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomainEntity).toList();
    }

    @Override
    public List<LoanOrder> findByStatus(OrderStatus status) {
        return jpaRepository.findByStatus(status).stream().map(this::toDomainEntity).toList();
    }

    @Override
    public List<LoanOrder> findByBeneficiaryId(Long beneficiaryId) {
        return jpaRepository.findByBeneficiaryId(beneficiaryId).stream().map(this::toDomainEntity).toList();
    }

    @Override
    public long countByStatus(OrderStatus status) {
        return jpaRepository.countByStatus(status);
    }

    public LoanOrderJpaEntity toJpaEntity(LoanOrder domain) {
        if (domain == null) return null;
        Objects.requireNonNull(domain.getBeneficiary(), "Solicitação sem beneficiário.");
        return LoanOrderJpaEntity.builder()
                .id(domain.getId())
                .beneficiary(beneficiaryJpaRepository.getReferenceById(domain.getBeneficiary().getId()))
                .forSelf(domain.isForSelf())
                .requesterName(domain.getRequesterName())
                .requesterRelationship(domain.getRequesterRelationship())
                .equipmentType(domain.getEquipmentType())
                .needReason(domain.getNeedReason())
                .status(domain.getStatus())
                .rejectionReason(domain.getRejectionReason())
                .assistedRegistration(domain.isAssistedRegistration())
                .decidedBy(domain.getDecidedBy())
                .decidedAt(domain.getDecidedAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public LoanOrder toDomainEntity(LoanOrderJpaEntity entity) {
        if (entity == null) return null;
        return LoanOrder.builder()
                .id(entity.getId())
                .beneficiary(beneficiaryAdapter.toDomainEntity(entity.getBeneficiary()))
                .forSelf(entity.isForSelf())
                .requesterName(entity.getRequesterName())
                .requesterRelationship(entity.getRequesterRelationship())
                .equipmentType(entity.getEquipmentType())
                .needReason(entity.getNeedReason())
                .status(entity.getStatus())
                .rejectionReason(entity.getRejectionReason())
                .assistedRegistration(entity.isAssistedRegistration())
                .decidedBy(entity.getDecidedBy())
                .decidedAt(entity.getDecidedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
