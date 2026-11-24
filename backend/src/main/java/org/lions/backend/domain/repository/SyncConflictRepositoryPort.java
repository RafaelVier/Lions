package org.lions.backend.domain.repository;

import org.lions.backend.domain.entity.SyncConflict;
import org.lions.backend.domain.enums.SyncConflictStatus;

import java.util.List;
import java.util.Optional;

public interface SyncConflictRepositoryPort {
    SyncConflict save(SyncConflict conflict);

    Optional<SyncConflict> findById(Long id);

    /** {@code status == null} = todos; mais recentes primeiro. */
    List<SyncConflict> findAll(SyncConflictStatus status);
}
