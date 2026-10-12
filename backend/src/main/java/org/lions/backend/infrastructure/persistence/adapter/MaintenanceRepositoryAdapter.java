package org.lions.backend.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.lions.backend.domain.entity.Maintenance;
import org.lions.backend.domain.enums.MaintenanceStatus;
import org.lions.backend.domain.repository.MaintenanceRepositoryPort;
import org.lions.backend.infrastructure.persistence.entity.MaintenanceJpaEntity;
import org.lions.backend.infrastructure.persistence.repository.MaintenanceJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MaintenanceRepositoryAdapter implements MaintenanceRepositoryPort {

    private final MaintenanceJpaRepository jpaRepository;
    private final EquipmentRepositoryAdapter equipmentAdapter;

    @Override
    public Maintenance save(Maintenance maintenance) {
        MaintenanceJpaEntity entity = toJpaEntity(maintenance);
        MaintenanceJpaEntity saved = jpaRepository.save(entity);
        return toDomainEntity(saved);
    }

    @Override
    public Optional<Maintenance> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomainEntity);
    }

    @Override
    public List<Maintenance> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomainEntity).toList();
    }

    @Override
    public List<Maintenance> findByEquipmentId(Long equipmentId) {
        return jpaRepository.findByEquipmentId(equipmentId).stream().map(this::toDomainEntity).toList();
    }

    @Override
    public List<Maintenance> findByStatus(MaintenanceStatus status) {
        return jpaRepository.findByStatus(status).stream().map(this::toDomainEntity).toList();
    }

    public MaintenanceJpaEntity toJpaEntity(Maintenance domain) {
        if (domain == null) return null;
        return MaintenanceJpaEntity.builder()
                .id(domain.getId())
                .equipment(equipmentAdapter.toJpaEntity(domain.getEquipment()))
                .startDate(domain.getStartDate())
                .endDate(domain.getEndDate())
                .description(domain.getDescription())
                .cost(domain.getCost())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public Maintenance toDomainEntity(MaintenanceJpaEntity entity) {
        if (entity == null) return null;
        return Maintenance.builder()
                .id(entity.getId())
                .equipment(equipmentAdapter.toDomainEntity(entity.getEquipment()))
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .description(entity.getDescription())
                .cost(entity.getCost())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
