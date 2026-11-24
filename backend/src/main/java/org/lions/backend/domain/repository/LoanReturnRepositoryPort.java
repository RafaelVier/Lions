package org.lions.backend.domain.repository;

import org.lions.backend.domain.entity.LoanReturn;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoanReturnRepositoryPort {
    LoanReturn save(LoanReturn loanReturn);

    Optional<LoanReturn> findById(Long id);

    List<LoanReturn> findAll();

    Optional<LoanReturn> findByLoanId(Long loanId);

    /** {@code returnDate} entre {@code start} e {@code end} (inclusive). */
    List<LoanReturn> findByReturnDateBetween(LocalDate start, LocalDate end);
}
