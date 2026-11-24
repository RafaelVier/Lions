package org.lions.backend.infrastructure.persistence.repository;

import org.lions.backend.domain.enums.SyncConflictStatus;
import org.lions.backend.infrastructure.persistence.entity.SyncConflictJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SyncConflictJpaRepository extends JpaRepository<SyncConflictJpaEntity, Long> {
    List<SyncConflictJpaEntity> findByStatusOrderByIdDesc(SyncConflictStatus status);

    List<SyncConflictJpaEntity> findAllByOrderByIdDesc();
}
