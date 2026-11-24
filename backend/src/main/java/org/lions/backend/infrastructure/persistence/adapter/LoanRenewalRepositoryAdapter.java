package org.lions.backend.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.lions.backend.domain.entity.LoanRenewal;
import org.lions.backend.domain.repository.LoanRenewalRepositoryPort;
import org.lions.backend.infrastructure.persistence.entity.LoanRenewalJpaEntity;
import org.lions.backend.infrastructure.persistence.repository.LoanRenewalJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanRenewalRepositoryAdapter implements LoanRenewalRepositoryPort {

    private final LoanRenewalJpaRepository jpaRepository;

    @Override
    @Transactional
    public LoanRenewal save(LoanRenewal renewal) {
        return toDomainEntity(jpaRepository.save(toJpaEntity(renewal)));
    }

    @Override
    public Optional<LoanRenewal> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomainEntity);
    }

    @Override
    public List<LoanRenewal> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomainEntity).toList();
    }

    @Override
    public List<LoanRenewal> findByLoanId(Long loanId) {
        return jpaRepository.findByLoanIdOrderByRenewedAtAsc(loanId).stream().map(this::toDomainEntity).toList();
    }

    public LoanRenewalJpaEntity toJpaEntity(LoanRenewal domain) {
        if (domain == null) return null;
        return LoanRenewalJpaEntity.builder()
                .id(domain.getId())
                .loanId(domain.getLoanId())
                .previousDueDate(domain.getPreviousDueDate())
                .newDueDate(domain.getNewDueDate())
                .renewedBy(domain.getRenewedBy())
                .renewedAt(domain.getRenewedAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public LoanRenewal toDomainEntity(LoanRenewalJpaEntity entity) {
        if (entity == null) return null;
        return LoanRenewal.builder()
                .id(entity.getId())
                .loanId(entity.getLoanId())
                .previousDueDate(entity.getPreviousDueDate())
                .newDueDate(entity.getNewDueDate())
                .renewedBy(entity.getRenewedBy())
                .renewedAt(entity.getRenewedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
