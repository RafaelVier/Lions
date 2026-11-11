package org.lions.backend.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.lions.backend.domain.entity.AuditLog;
import org.lions.backend.domain.repository.AuditLogRepositoryPort;
import org.lions.backend.infrastructure.persistence.entity.AuditLogJpaEntity;
import org.lions.backend.infrastructure.persistence.repository.AuditLogJpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogRepositoryAdapter implements AuditLogRepositoryPort {

    private final AuditLogJpaRepository jpaRepository;

    @Override
    @Transactional
    public AuditLog save(AuditLog auditLog) {
        return toDomainEntity(jpaRepository.save(toJpaEntity(auditLog)));
    }

    @Override
    public List<AuditLog> search(String entityName, String entityId, LocalDateTime from, LocalDateTime to) {
        Specification<AuditLogJpaEntity> spec = Specification.where(null);
        if (entityName != null && !entityName.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entityName"), entityName));
        }
        if (entityId != null && !entityId.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entityId"), entityId));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("occurredAt"), to));
        }
        return jpaRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "occurredAt", "id")).stream()
                .map(this::toDomainEntity).toList();
    }

    public AuditLogJpaEntity toJpaEntity(AuditLog domain) {
        if (domain == null) return null;
        return AuditLogJpaEntity.builder()
                .id(domain.getId())
                .entityName(domain.getEntityName())
                .entityId(domain.getEntityId())
                .action(domain.getAction())
                .username(domain.getUsername())
                .automatic(domain.isAutomatic())
                .originRule(domain.getOriginRule())
                .details(domain.getDetails())
                .occurredAt(domain.getOccurredAt())
                .build();
    }

    public AuditLog toDomainEntity(AuditLogJpaEntity entity) {
        if (entity == null) return null;
        return AuditLog.builder()
                .id(entity.getId())
                .entityName(entity.getEntityName())
                .entityId(entity.getEntityId())
                .action(entity.getAction())
                .username(entity.getUsername())
                .automatic(entity.isAutomatic())
                .originRule(entity.getOriginRule())
                .details(entity.getDetails())
                .occurredAt(entity.getOccurredAt())
                .build();
    }
}
