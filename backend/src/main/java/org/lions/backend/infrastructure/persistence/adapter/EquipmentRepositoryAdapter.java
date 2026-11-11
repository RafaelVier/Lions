package org.lions.backend.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.lions.backend.domain.entity.Equipment;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.repository.EquipmentRepositoryPort;
import org.lions.backend.infrastructure.persistence.entity.EquipmentJpaEntity;
import org.lions.backend.infrastructure.persistence.repository.EquipmentJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipmentRepositoryAdapter implements EquipmentRepositoryPort {

    private static final String PROVISIONAL_PATRIMONY_PREFIX = "TMP-";

    private final EquipmentJpaRepository jpaRepository;

    @Override
    @Transactional
    public Equipment save(Equipment equipment) {
        if (equipment.getId() == null) {
            return saveNew(equipment);
        }
        EquipmentJpaEntity entity = toJpaEntity(equipment);
        if (entity.getVersion() == null) {
            // Sem versão conhecida não há como detectar edição concorrente; usa a versão atual do banco.
            entity.setVersion(jpaRepository.findById(entity.getId())
                    .map(EquipmentJpaEntity::getVersion)
                    .orElse(null));
        }
        return toDomainEntity(jpaRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public Equipment saveNew(Equipment equipment) {
        if (equipment.getId() != null) {
            throw new IllegalArgumentException("saveNew só aceita equipamentos ainda não persistidos.");
        }
        EquipmentJpaEntity entity = toJpaEntity(equipment);
        entity.setVersion(null);
        if (entity.getStatus() == null) {
            entity.setStatus(EquipmentStatus.DISPONIVEL);
        }
        // Valor provisório único até o id (IDENTITY) existir; substituído por PT-%04d na mesma transação.
        entity.setPatrimonyNumber(PROVISIONAL_PATRIMONY_PREFIX + UUID.randomUUID().toString().replace("-", ""));
        EquipmentJpaEntity saved = jpaRepository.saveAndFlush(entity);
        saved.setPatrimonyNumber(Equipment.formatPatrimony(saved.getId()));
        return toDomainEntity(jpaRepository.saveAndFlush(saved));
    }

    @Override
    public Optional<Equipment> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomainEntity);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Equipment> findByIdForUpdate(Long id) {
        return jpaRepository.findByIdForUpdate(id).map(this::toDomainEntity);
    }

    @Override
    public List<Equipment> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomainEntity).toList();
    }

    @Override
    public Optional<Equipment> findByPatrimonyNumber(String patrimonyNumber) {
        return jpaRepository.findByPatrimonyNumber(patrimonyNumber).map(this::toDomainEntity);
    }

    @Override
    public List<Equipment> findByStatus(EquipmentStatus status) {
        return jpaRepository.findByStatus(status).stream().map(this::toDomainEntity).toList();
    }

    @Override
    public List<Equipment> findByType(EquipmentType type) {
        return jpaRepository.findByType(type).stream().map(this::toDomainEntity).toList();
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
    public long countByTypeAndStatus(EquipmentType type, EquipmentStatus status) {
        return jpaRepository.countByTypeAndStatus(type, status);
    }

    @Override
    public long countTotal() {
        return jpaRepository.count();
    }

    @Override
    public List<Equipment> findAllActive() {
        return jpaRepository.findByStatusNot(EquipmentStatus.BAIXADO).stream().map(this::toDomainEntity).toList();
    }

    public EquipmentJpaEntity toJpaEntity(Equipment domain) {
        if (domain == null) return null;
        return EquipmentJpaEntity.builder()
                .id(domain.getId())
                .patrimonyNumber(domain.getPatrimonyNumber())
                .type(domain.getType())
                .size(domain.getSize())
                .condition(domain.getCondition())
                .status(domain.getStatus())
                .notes(domain.getNotes())
                .photoUrl(domain.getPhotoUrl())
                .reservedForBeneficiaryId(domain.getReservedForBeneficiaryId())
                .writeOffReason(domain.getWriteOffReason())
                .writeOffNotes(domain.getWriteOffNotes())
                .writeOffDate(domain.getWriteOffDate())
                .writeOffBy(domain.getWriteOffBy())
                .lastMaintenanceDate(domain.getLastMaintenanceDate())
                .loanCyclesSinceMaintenance(domain.getLoanCyclesSinceMaintenance())
                .version(domain.getVersion())
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
                .size(entity.getSize())
                .condition(entity.getCondition())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .photoUrl(entity.getPhotoUrl())
                .reservedForBeneficiaryId(entity.getReservedForBeneficiaryId())
                .writeOffReason(entity.getWriteOffReason())
                .writeOffNotes(entity.getWriteOffNotes())
                .writeOffDate(entity.getWriteOffDate())
                .writeOffBy(entity.getWriteOffBy())
                .lastMaintenanceDate(entity.getLastMaintenanceDate())
                .loanCyclesSinceMaintenance(entity.getLoanCyclesSinceMaintenance())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
