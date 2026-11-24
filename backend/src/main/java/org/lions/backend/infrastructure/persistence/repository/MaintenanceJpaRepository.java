package org.lions.backend.infrastructure.persistence.repository;

import org.lions.backend.domain.enums.MaintenanceStatus;
import org.lions.backend.infrastructure.persistence.entity.MaintenanceJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceJpaRepository extends JpaRepository<MaintenanceJpaEntity, Long> {
    @EntityGraph(attributePaths = "equipment")
    List<MaintenanceJpaEntity> findByEquipmentIdOrderByEntryDateDesc(Long equipmentId);

    @EntityGraph(attributePaths = "equipment")
    List<MaintenanceJpaEntity> findByStatusOrderByEntryDateDesc(MaintenanceStatus status);

    @EntityGraph(attributePaths = "equipment")
    List<MaintenanceJpaEntity> findByEntryDateBetweenOrderByEntryDate(LocalDate start, LocalDate end);

    @EntityGraph(attributePaths = "equipment")
    Optional<MaintenanceJpaEntity> findFirstByEquipmentIdAndStatusInOrderByExitDateDescIdDesc(
            Long equipmentId, Collection<MaintenanceStatus> statuses);
}
