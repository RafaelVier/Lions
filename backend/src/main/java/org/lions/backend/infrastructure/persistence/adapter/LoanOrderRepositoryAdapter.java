package org.lions.backend.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.lions.backend.domain.entity.LoanOrder;
import org.lions.backend.domain.enums.OrderStatus;
import org.lions.backend.domain.repository.LoanOrderRepositoryPort;
import org.lions.backend.infrastructure.persistence.entity.LoanOrderJpaEntity;
import org.lions.backend.infrastructure.persistence.repository.LoanOrderJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LoanOrderRepositoryAdapter implements LoanOrderRepositoryPort {

    private final LoanOrderJpaRepository jpaRepository;
    private final RequesterRepositoryAdapter requesterAdapter;

    @Override
    public LoanOrder save(LoanOrder order) {
        LoanOrderJpaEntity entity = toJpaEntity(order);
        LoanOrderJpaEntity saved = jpaRepository.save(entity);
        return toDomainEntity(saved);
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
    public long countByStatus(OrderStatus status) {
        return jpaRepository.countByStatus(status);
    }

    public LoanOrderJpaEntity toJpaEntity(LoanOrder domain) {
        if (domain == null) return null;
        return LoanOrderJpaEntity.builder()
                .id(domain.getId())
                .requester(requesterAdapter.toJpaEntity(domain.getRequester()))
                .equipmentType(domain.getEquipmentType())
                .quantity(domain.getQuantity())
                .status(domain.getStatus())
                .rejectionReason(domain.getRejectionReason())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public LoanOrder toDomainEntity(LoanOrderJpaEntity entity) {
        if (entity == null) return null;
        return LoanOrder.builder()
                .id(entity.getId())
                .requester(requesterAdapter.toDomainEntity(entity.getRequester()))
                .equipmentType(entity.getEquipmentType())
                .quantity(entity.getQuantity())
                .status(entity.getStatus())
                .rejectionReason(entity.getRejectionReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
