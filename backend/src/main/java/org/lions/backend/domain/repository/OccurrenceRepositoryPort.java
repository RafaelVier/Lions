package org.lions.backend.domain.repository;

import org.lions.backend.domain.entity.Occurrence;
import org.lions.backend.domain.enums.OccurrenceStatus;
import org.lions.backend.domain.enums.OccurrenceType;

import java.util.List;
import java.util.Optional;

public interface OccurrenceRepositoryPort {
    Occurrence save(Occurrence occurrence);

    Optional<Occurrence> findById(Long id);

    List<Occurrence> findAll();

    List<Occurrence> findByBeneficiaryId(Long beneficiaryId);

    List<Occurrence> findByStatus(OccurrenceStatus status);

    boolean existsByBeneficiaryIdAndStatus(Long beneficiaryId, OccurrenceStatus status);

    boolean existsByLoanIdAndType(Long loanId, OccurrenceType type);
}
