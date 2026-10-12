package org.lions.backend.domain.repository;

import org.lions.backend.domain.entity.Loan;
import org.lions.backend.domain.enums.LoanStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoanRepositoryPort {
    Loan save(Loan loan);
    Optional<Loan> findById(Long id);
    List<Loan> findAll();
    List<Loan> findByStatus(LoanStatus status);
    List<Loan> findDueBetween(LocalDate start, LocalDate end);
    List<Loan> findOverdue(LocalDate cutoffDate);
    List<Loan> findByEquipmentId(Long equipmentId);
    List<Loan> findByRequesterId(Long requesterId);
    long countByStatus(LoanStatus status);
}
