package org.lions.backend.infrastructure.persistence.repository;

import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.infrastructure.persistence.entity.EquipmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentJpaRepository extends JpaRepository<EquipmentJpaEntity, Long> {
    Optional<EquipmentJpaEntity> findByPatrimonyNumber(String patrimonyNumber);
    List<EquipmentJpaEntity> findByStatus(EquipmentStatus status);
    List<EquipmentJpaEntity> findByTypeAndStatus(EquipmentType type, EquipmentStatus status);
    long countByStatus(EquipmentStatus status);
    boolean existsByPatrimonyNumber(String patrimonyNumber);
}
