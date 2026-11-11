package org.lions.backend.domain.repository;

import org.lions.backend.domain.entity.LoanRenewal;

import java.util.List;
import java.util.Optional;

public interface LoanRenewalRepositoryPort {
    LoanRenewal save(LoanRenewal renewal);

    Optional<LoanRenewal> findById(Long id);

    List<LoanRenewal> findAll();

    /** Ordenado por {@code renewedAt} crescente. */
    List<LoanRenewal> findByLoanId(Long loanId);
}
