package org.lions.backend.infrastructure.persistence.repository;

import org.lions.backend.infrastructure.persistence.entity.LoanReturnJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanReturnJpaRepository extends JpaRepository<LoanReturnJpaEntity, Long> {
    Optional<LoanReturnJpaEntity> findByLoanId(Long loanId);

    List<LoanReturnJpaEntity> findByReturnDateBetweenOrderByReturnDate(LocalDate start, LocalDate end);
}
