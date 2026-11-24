package org.lions.backend.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.lions.backend.domain.entity.EquipmentTypeConfig;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.repository.EquipmentTypeConfigRepositoryPort;
import org.lions.backend.infrastructure.persistence.entity.EquipmentTypeJpaEntity;
import org.lions.backend.infrastructure.persistence.repository.EquipmentTypeJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipmentTypeConfigRepositoryAdapter implements EquipmentTypeConfigRepositoryPort {

    private final EquipmentTypeJpaRepository jpaRepository;

    @Override
    public Optional<EquipmentTypeConfig> findByCode(EquipmentType code) {
        return jpaRepository.findById(code).map(this::toDomainEntity);
    }

    @Override
    public List<EquipmentTypeConfig> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomainEntity)
                .sorted(Comparator.comparing(EquipmentTypeConfig::getCode))
                .toList();
    }

    @Override
    @Transactional
    public EquipmentTypeConfig save(EquipmentTypeConfig config) {
        return toDomainEntity(jpaRepository.save(toJpaEntity(config)));
    }

    private EquipmentTypeJpaEntity toJpaEntity(EquipmentTypeConfig domain) {
        return EquipmentTypeJpaEntity.builder()
                .code(domain.getCode())
                .description(domain.getDescription())
                .loanDays(domain.getLoanDays())
                .updatedBy(domain.getUpdatedBy())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private EquipmentTypeConfig toDomainEntity(EquipmentTypeJpaEntity entity) {
        return EquipmentTypeConfig.builder()
                .code(entity.getCode())
                .description(entity.getDescription())
                .loanDays(entity.getLoanDays())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
