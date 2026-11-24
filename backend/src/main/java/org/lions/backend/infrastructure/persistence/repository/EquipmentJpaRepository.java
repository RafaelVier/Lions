package org.lions.backend.infrastructure.persistence.repository;

import jakarta.persistence.LockModeType;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.infrastructure.persistence.entity.EquipmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentJpaRepository extends JpaRepository<EquipmentJpaEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EquipmentJpaEntity e WHERE e.id = :id")
    Optional<EquipmentJpaEntity> findByIdForUpdate(@Param("id") Long id);

    Optional<EquipmentJpaEntity> findByPatrimonyNumber(String patrimonyNumber);

    List<EquipmentJpaEntity> findByStatus(EquipmentStatus status);

    List<EquipmentJpaEntity> findByType(EquipmentType type);

    List<EquipmentJpaEntity> findByTypeAndStatus(EquipmentType type, EquipmentStatus status);

    List<EquipmentJpaEntity> findByStatusNot(EquipmentStatus status);

    long countByStatus(EquipmentStatus status);

    long countByTypeAndStatus(EquipmentType type, EquipmentStatus status);
}
