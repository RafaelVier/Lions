package org.lions.backend.domain.repository;

import org.lions.backend.domain.entity.LoanReturn;

import java.util.List;
import java.util.Optional;

public interface LoanReturnRepositoryPort {
    LoanReturn save(LoanReturn loanReturn);
    Optional<LoanReturn> findById(Long id);
    Optional<LoanReturn> findByLoanId(Long loanId);
    List<LoanReturn> findAll();
}
