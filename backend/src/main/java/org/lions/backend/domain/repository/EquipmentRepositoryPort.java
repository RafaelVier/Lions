package org.lions.backend.domain.repository;

import org.lions.backend.domain.entity.Equipment;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.EquipmentType;

import java.util.List;
import java.util.Optional;

public interface EquipmentRepositoryPort {
    Equipment save(Equipment equipment);
    Optional<Equipment> findById(Long id);
    Optional<Equipment> findByPatrimonyNumber(String patrimonyNumber);
    List<Equipment> findAll();
    List<Equipment> findByStatus(EquipmentStatus status);
    List<Equipment> findByTypeAndStatus(EquipmentType type, EquipmentStatus status);
    long countByStatus(EquipmentStatus status);
    long countTotal();
    boolean existsByPatrimonyNumber(String patrimonyNumber);
    void deleteById(Long id);
}
