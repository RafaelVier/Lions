package org.lions.backend.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.lions.backend.domain.entity.SystemParameter;
import org.lions.backend.domain.repository.SystemParameterRepositoryPort;
import org.lions.backend.infrastructure.persistence.entity.SystemParameterJpaEntity;
import org.lions.backend.infrastructure.persistence.repository.SystemParameterJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemParameterRepositoryAdapter implements SystemParameterRepositoryPort {

    private final SystemParameterJpaRepository jpaRepository;

    @Override
    public Optional<SystemParameter> findByKey(String key) {
        return jpaRepository.findById(key).map(this::toDomainEntity);
    }

    @Override
    public List<SystemParameter> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomainEntity).toList();
    }

    @Override
    @Transactional
    public SystemParameter save(SystemParameter parameter) {
        return toDomainEntity(jpaRepository.save(toJpaEntity(parameter)));
    }

    public SystemParameterJpaEntity toJpaEntity(SystemParameter domain) {
        if (domain == null) return null;
        return SystemParameterJpaEntity.builder()
                .key(domain.getKey())
                .value(domain.getValue())
                .description(domain.getDescription())
                .updatedBy(domain.getUpdatedBy())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public SystemParameter toDomainEntity(SystemParameterJpaEntity entity) {
        if (entity == null) return null;
        return SystemParameter.builder()
                .key(entity.getKey())
                .value(entity.getValue())
                .description(entity.getDescription())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
