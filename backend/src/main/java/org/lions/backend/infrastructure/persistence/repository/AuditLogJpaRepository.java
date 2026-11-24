package org.lions.backend.infrastructure.persistence.repository;

import org.lions.backend.infrastructure.persistence.entity.AuditLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogJpaRepository extends JpaRepository<AuditLogJpaEntity, Long>,
        JpaSpecificationExecutor<AuditLogJpaEntity> {
}
