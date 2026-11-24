package org.lions.backend.infrastructure.persistence.repository;

import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.infrastructure.persistence.entity.EquipmentTypeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipmentTypeJpaRepository extends JpaRepository<EquipmentTypeJpaEntity, EquipmentType> {
}
