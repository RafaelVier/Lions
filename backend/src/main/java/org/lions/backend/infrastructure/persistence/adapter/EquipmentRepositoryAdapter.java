package org.lions.backend.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.lions.backend.domain.entity.Equipment;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.repository.EquipmentRepositoryPort;
import org.lions.backend.infrastructure.persistence.entity.EquipmentJpaEntity;
import org.lions.backend.infrastructure.persistence.repository.EquipmentJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EquipmentRepositoryAdapter implements EquipmentRepositoryPort {

    private final EquipmentJpaRepository jpaRepository;

    @Override
    public Equipment save(Equipment equipment) {
        EquipmentJpaEntity entity = toJpaEntity(equipment);
        EquipmentJpaEntity saved = jpaRepository.save(entity);
        return toDomainEntity(saved);
    }

    @Override
    public Optional<Equipment> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomainEntity);
    }

    @Override
    public Optional<Equipment> findByPatrimonyNumber(String patrimonyNumber) {
        return jpaRepository.findByPatrimonyNumber(patrimonyNumber).map(this::toDomainEntity);
    }

    @Override
    public List<Equipment> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomainEntity).toList();
    }

    @Override
    public List<Equipment> findByStatus(EquipmentStatus status) {
        return jpaRepository.findByStatus(status).stream().map(this::toDomainEntity).toList();
    }

    @Override
    public List<Equipment> findByTypeAndStatus(EquipmentType type, EquipmentStatus status) {
        return jpaRepository.findByTypeAndStatus(type, status).stream().map(this::toDomainEntity).toList();
    }

    @Override
    public long countByStatus(EquipmentStatus status) {
        return jpaRepository.countByStatus(status);
    }

    @Override
    public long countTotal() {
        return jpaRepository.count();
    }

    @Override
    public boolean existsByPatrimonyNumber(String patrimonyNumber) {
        return jpaRepository.existsByPatrimonyNumber(patrimonyNumber);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    public EquipmentJpaEntity toJpaEntity(Equipment domain) {
        if (domain == null) return null;
        return EquipmentJpaEntity.builder()
                .id(domain.getId())
                .patrimonyNumber(domain.getPatrimonyNumber())
                .type(domain.getType())
                .condition(domain.getCondition())
                .status(domain.getStatus())
                .notes(domain.getNotes())
                .photoUrl(domain.getPhotoUrl())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public Equipment toDomainEntity(EquipmentJpaEntity entity) {
        if (entity == null) return null;
        return Equipment.builder()
                .id(entity.getId())
                .patrimonyNumber(entity.getPatrimonyNumber())
                .type(entity.getType())
                .condition(entity.getCondition())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .photoUrl(entity.getPhotoUrl())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
