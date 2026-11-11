package org.lions.backend.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.lions.backend.domain.entity.LoanReturn;
import org.lions.backend.domain.repository.LoanReturnRepositoryPort;
import org.lions.backend.infrastructure.persistence.entity.LoanReturnJpaEntity;
import org.lions.backend.infrastructure.persistence.repository.LoanJpaRepository;
import org.lions.backend.infrastructure.persistence.repository.LoanReturnJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanReturnRepositoryAdapter implements LoanReturnRepositoryPort {

    private final LoanReturnJpaRepository jpaRepository;
    private final LoanJpaRepository loanJpaRepository;

    @Override
    @Transactional
    public LoanReturn save(LoanReturn loanReturn) {
        return toDomainEntity(jpaRepository.save(toJpaEntity(loanReturn)));
    }

    @Override
    public Optional<LoanReturn> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomainEntity);
    }

    @Override
    public List<LoanReturn> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomainEntity).toList();
    }

    @Override
    public Optional<LoanReturn> findByLoanId(Long loanId) {
        return jpaRepository.findByLoanId(loanId).map(this::toDomainEntity);
    }

    @Override
    public List<LoanReturn> findByReturnDateBetween(LocalDate start, LocalDate end) {
        return jpaRepository.findByReturnDateBetweenOrderByReturnDate(start, end).stream()
                .map(this::toDomainEntity).toList();
    }

    public LoanReturnJpaEntity toJpaEntity(LoanReturn domain) {
        if (domain == null) return null;
        Objects.requireNonNull(domain.getLoanId(), "Devolução sem empréstimo.");
        return LoanReturnJpaEntity.builder()
                .id(domain.getId())
                .loan(loanJpaRepository.getReferenceById(domain.getLoanId()))
                .returnDate(domain.getReturnDate())
                .returnCondition(domain.getReturnCondition())
                .damageDescription(domain.getDamageDescription())
                .returnPhotoUrl(domain.getReturnPhotoUrl())
                .hasDamage(domain.isHasDamage())
                .receivedBy(domain.getReceivedBy())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public LoanReturn toDomainEntity(LoanReturnJpaEntity entity) {
        if (entity == null) return null;
        return LoanReturn.builder()
                .id(entity.getId())
                .loanId(entity.getLoan() == null ? null : entity.getLoan().getId())
                .returnDate(entity.getReturnDate())
                .returnCondition(entity.getReturnCondition())
                .damageDescription(entity.getDamageDescription())
                .returnPhotoUrl(entity.getReturnPhotoUrl())
                .hasDamage(entity.isHasDamage())
                .receivedBy(entity.getReceivedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
