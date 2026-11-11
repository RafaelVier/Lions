package org.lions.backend.domain.repository;

import org.lions.backend.domain.entity.Loan;
import org.lions.backend.domain.enums.LoanStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** "Open" = status PENDENTE_ASSINATURA, ATIVO ou ATRASADO ({@link LoanStatus#isOpen()}). */
public interface LoanRepositoryPort {
    Loan save(Loan loan);

    Optional<Loan> findById(Long id);

    List<Loan> findAll();

    List<Loan> findByStatus(LoanStatus status);

    List<Loan> findByStatusIn(Collection<LoanStatus> statuses);

    List<Loan> findByBeneficiaryId(Long beneficiaryId);

    List<Loan> findByEquipmentId(Long equipmentId);

    List<Loan> findOpenByBeneficiaryId(Long beneficiaryId);

    /** Em aberto com {@code expectedReturnDate} entre {@code start} e {@code end} (inclusive). */
    List<Loan> findOpenDueBetween(LocalDate start, LocalDate end);

    /** Em aberto com {@code expectedReturnDate < today}. */
    List<Loan> findOpenOverdue(LocalDate today);

    /** {@code loanDate} entre {@code start} e {@code end} (inclusive). */
    List<Loan> findByLoanDateBetween(LocalDate start, LocalDate end);

    long countByStatus(LoanStatus status);

    long countByLoanDateBetween(LocalDate start, LocalDate end);
}
