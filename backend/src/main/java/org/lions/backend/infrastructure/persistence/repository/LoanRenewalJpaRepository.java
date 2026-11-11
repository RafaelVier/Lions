package org.lions.backend.infrastructure.persistence.repository;

import org.lions.backend.infrastructure.persistence.entity.LoanRenewalJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRenewalJpaRepository extends JpaRepository<LoanRenewalJpaEntity, Long> {
    List<LoanRenewalJpaEntity> findByLoanIdOrderByRenewedAtAsc(Long loanId);
}
