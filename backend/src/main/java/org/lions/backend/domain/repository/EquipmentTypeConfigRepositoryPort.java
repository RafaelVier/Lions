package org.lions.backend.domain.repository;

import org.lions.backend.domain.entity.EquipmentTypeConfig;
import org.lions.backend.domain.enums.EquipmentType;

import java.util.List;
import java.util.Optional;

public interface EquipmentTypeConfigRepositoryPort {
    Optional<EquipmentTypeConfig> findByCode(EquipmentType code);

    List<EquipmentTypeConfig> findAll();

    EquipmentTypeConfig save(EquipmentTypeConfig config);
}
