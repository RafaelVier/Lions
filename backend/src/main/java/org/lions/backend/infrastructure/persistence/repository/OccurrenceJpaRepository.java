package org.lions.backend.infrastructure.persistence.repository;

import org.lions.backend.domain.enums.OccurrenceStatus;
import org.lions.backend.domain.enums.OccurrenceType;
import org.lions.backend.infrastructure.persistence.entity.OccurrenceJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OccurrenceJpaRepository extends JpaRepository<OccurrenceJpaEntity, Long> {
    @EntityGraph(attributePaths = "beneficiary")
    List<OccurrenceJpaEntity> findByBeneficiaryIdOrderByCreatedAtDesc(Long beneficiaryId);

    @EntityGraph(attributePaths = "beneficiary")
    List<OccurrenceJpaEntity> findByStatusOrderByCreatedAtDesc(OccurrenceStatus status);

    boolean existsByBeneficiaryIdAndStatus(Long beneficiaryId, OccurrenceStatus status);

    boolean existsByLoanIdAndType(Long loanId, OccurrenceType type);
}
