package org.lions.backend.domain.repository;

import org.lions.backend.domain.entity.Maintenance;
import org.lions.backend.domain.enums.MaintenanceStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MaintenanceRepositoryPort {
    /** Persiste a manutenção e seus custos (cascade). */
    Maintenance save(Maintenance maintenance);

    Optional<Maintenance> findById(Long id);

    List<Maintenance> findAll();

    List<Maintenance> findByEquipmentId(Long equipmentId);

    List<Maintenance> findByStatus(MaintenanceStatus status);

    /** {@code entryDate} entre {@code start} e {@code end} (inclusive). */
    List<Maintenance> findByEntryDateBetween(LocalDate start, LocalDate end);

    /** Última manutenção encerrada (maior {@code exitDate}) do equipamento. */
    Optional<Maintenance> findLastClosedByEquipmentId(Long equipmentId);
}
