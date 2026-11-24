package org.lions.backend.domain.repository;

import org.lions.backend.domain.entity.Equipment;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.EquipmentType;

import java.util.List;
import java.util.Optional;

public interface EquipmentRepositoryPort {
    /** Atualiza (controle otimista por {@code version}); se {@code id == null}, delega para {@link #saveNew}. */
    Equipment save(Equipment equipment);

    /** Persiste um equipamento novo e atribui o patrimônio {@code PT-%04d} a partir do id, na mesma transação. */
    Equipment saveNew(Equipment equipment);

    Optional<Equipment> findById(Long id);

    /** Leitura com lock pessimista (PESSIMISTIC_WRITE) — use dentro de transação. */
    Optional<Equipment> findByIdForUpdate(Long id);

    List<Equipment> findAll();

    Optional<Equipment> findByPatrimonyNumber(String patrimonyNumber);

    List<Equipment> findByStatus(EquipmentStatus status);

    List<Equipment> findByType(EquipmentType type);

    List<Equipment> findByTypeAndStatus(EquipmentType type, EquipmentStatus status);

    long countByStatus(EquipmentStatus status);

    long countByTypeAndStatus(EquipmentType type, EquipmentStatus status);

    long countTotal();

    /** Equipamentos não baixados (status ≠ BAIXADO). */
    List<Equipment> findAllActive();
}
