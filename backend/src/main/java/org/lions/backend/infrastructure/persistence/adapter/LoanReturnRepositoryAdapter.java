package org.lions.backend.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.lions.backend.domain.entity.LoanReturn;
import org.lions.backend.domain.repository.LoanReturnRepositoryPort;
import org.lions.backend.infrastructure.persistence.entity.LoanReturnJpaEntity;
import org.lions.backend.infrastructure.persistence.repository.LoanReturnJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LoanReturnRepositoryAdapter implements LoanReturnRepositoryPort {

    private final LoanReturnJpaRepository jpaRepository;
    private final LoanRepositoryAdapter loanAdapter;

    @Override
    public LoanReturn save(LoanReturn loanReturn) {
        LoanReturnJpaEntity entity = toJpaEntity(loanReturn);
        LoanReturnJpaEntity saved = jpaRepository.save(entity);
        return toDomainEntity(saved);
    }

    @Override
    public Optional<LoanReturn> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomainEntity);
    }

    @Override
    public Optional<LoanReturn> findByLoanId(Long loanId) {
        return jpaRepository.findByLoanId(loanId).map(this::toDomainEntity);
    }

    @Override
    public List<LoanReturn> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomainEntity).toList();
    }

    public LoanReturnJpaEntity toJpaEntity(LoanReturn domain) {
        if (domain == null) return null;
        return LoanReturnJpaEntity.builder()
                .id(domain.getId())
                .loan(loanAdapter.toJpaEntity(domain.getLoan()))
                .returnDate(domain.getReturnDate())
                .returnCondition(domain.getReturnCondition())
                .damageDescription(domain.getDamageDescription())
                .returnPhotoUrl(domain.getReturnPhotoUrl())
                .hasDamage(domain.isHasDamage())
                .responsibilityAssigned(domain.isResponsibilityAssigned())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public LoanReturn toDomainEntity(LoanReturnJpaEntity entity) {
        if (entity == null) return null;
        return LoanReturn.builder()
                .id(entity.getId())
                .loan(loanAdapter.toDomainEntity(entity.getLoan()))
                .returnDate(entity.getReturnDate())
                .returnCondition(entity.getReturnCondition())
                .damageDescription(entity.getDamageDescription())
                .returnPhotoUrl(entity.getReturnPhotoUrl())
                .hasDamage(entity.isHasDamage())
                .responsibilityAssigned(entity.isResponsibilityAssigned())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
