package org.lions.backend.infrastructure.persistence.repository;

import org.lions.backend.domain.enums.MaintenanceStatus;
import org.lions.backend.infrastructure.persistence.entity.MaintenanceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceJpaRepository extends JpaRepository<MaintenanceJpaEntity, Long> {
    List<MaintenanceJpaEntity> findByEquipmentId(Long equipmentId);
    List<MaintenanceJpaEntity> findByStatus(MaintenanceStatus status);
}
