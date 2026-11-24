package org.lions.backend.infrastructure.persistence.repository;

import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.WaitlistStatus;
import org.lions.backend.infrastructure.persistence.entity.WaitlistEntryJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface WaitlistEntryJpaRepository extends JpaRepository<WaitlistEntryJpaEntity, Long> {
    @EntityGraph(attributePaths = "beneficiary")
    List<WaitlistEntryJpaEntity> findByEquipmentTypeAndStatusOrderByPriorityDescQueuedAtAsc(EquipmentType type,
                                                                                         WaitlistStatus status);

    @EntityGraph(attributePaths = "beneficiary")
    List<WaitlistEntryJpaEntity> findByStatusOrderByQueuedAtAsc(WaitlistStatus status);

    @EntityGraph(attributePaths = "beneficiary")
    List<WaitlistEntryJpaEntity> findByBeneficiaryIdOrderByCreatedAtDesc(Long beneficiaryId);

    @EntityGraph(attributePaths = "beneficiary")
    List<WaitlistEntryJpaEntity> findByBeneficiaryIdAndStatusIn(Long beneficiaryId,
                                                                Collection<WaitlistStatus> statuses);

    boolean existsByBeneficiaryIdAndEquipmentTypeAndStatusIn(Long beneficiaryId, EquipmentType type,
                                                             Collection<WaitlistStatus> statuses);

    @EntityGraph(attributePaths = "beneficiary")
    List<WaitlistEntryJpaEntity> findByStatusAndReservationExpiresAtBefore(WaitlistStatus status,
                                                                          LocalDateTime now);

    long countByEquipmentTypeAndStatus(EquipmentType type, WaitlistStatus status);
}
