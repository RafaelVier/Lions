package org.lions.backend.domain.repository;

import org.lions.backend.domain.entity.Maintenance;
import org.lions.backend.domain.enums.MaintenanceStatus;

import java.util.List;
import java.util.Optional;

public interface MaintenanceRepositoryPort {
    Maintenance save(Maintenance maintenance);
    Optional<Maintenance> findById(Long id);
    List<Maintenance> findAll();
    List<Maintenance> findByEquipmentId(Long equipmentId);
    List<Maintenance> findByStatus(MaintenanceStatus status);
}
