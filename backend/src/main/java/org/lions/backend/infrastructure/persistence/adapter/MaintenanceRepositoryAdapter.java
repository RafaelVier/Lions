package org.lions.backend.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.lions.backend.domain.entity.Maintenance;
import org.lions.backend.domain.entity.MaintenanceCost;
import org.lions.backend.domain.enums.MaintenanceStatus;
import org.lions.backend.domain.repository.MaintenanceRepositoryPort;
import org.lions.backend.infrastructure.persistence.entity.MaintenanceCostJpaEntity;
import org.lions.backend.infrastructure.persistence.entity.MaintenanceJpaEntity;
import org.lions.backend.infrastructure.persistence.repository.EquipmentJpaRepository;
import org.lions.backend.infrastructure.persistence.repository.MaintenanceJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Custos persistidos em {@code tb_maintenance_costs} como filhos de {@code tb_maintenances}
 * ({@code @OneToMany(cascade = ALL, orphanRemoval = true)}): salvar a manutenção salva os custos.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaintenanceRepositoryAdapter implements MaintenanceRepositoryPort {

    private final MaintenanceJpaRepository jpaRepository;
    private final EquipmentJpaRepository equipmentJpaRepository;
    private final EquipmentRepositoryAdapter equipmentAdapter;

    @Override
    @Transactional
    public Maintenance save(Maintenance maintenance) {
        return toDomainEntity(jpaRepository.saveAndFlush(toJpaEntity(maintenance)));
    }

    @Override
    public Optional<Maintenance> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomainEntity);
    }

    @Override
    public List<Maintenance> findAll() {
        return map(jpaRepository.findAll());
    }

    @Override
    public List<Maintenance> findByEquipmentId(Long equipmentId) {
        return map(jpaRepository.findByEquipmentIdOrderByEntryDateDesc(equipmentId));
    }

    @Override
    public List<Maintenance> findByStatus(MaintenanceStatus status) {
        return map(jpaRepository.findByStatusOrderByEntryDateDesc(status));
    }

    @Override
    public List<Maintenance> findByEntryDateBetween(LocalDate start, LocalDate end) {
        return map(jpaRepository.findByEntryDateBetweenOrderByEntryDate(start, end));
    }

    @Override
    public Optional<Maintenance> findLastClosedByEquipmentId(Long equipmentId) {
        return jpaRepository.findFirstByEquipmentIdAndStatusInOrderByExitDateDescIdDesc(equipmentId,
                        EnumSet.of(MaintenanceStatus.CONCLUIDA_APTO, MaintenanceStatus.CONCLUIDA_INSERVIVEL))
                .map(this::toDomainEntity);
    }

    private List<Maintenance> map(List<MaintenanceJpaEntity> entities) {
        return entities.stream().map(this::toDomainEntity).toList();
    }

    public MaintenanceJpaEntity toJpaEntity(Maintenance domain) {
        if (domain == null) return null;
        Objects.requireNonNull(domain.getEquipment(), "Manutenção sem equipamento.");
        MaintenanceJpaEntity entity = MaintenanceJpaEntity.builder()
                .id(domain.getId())
                .equipment(equipmentJpaRepository.getReferenceById(domain.getEquipment().getId()))
                .type(domain.getType())
                .status(domain.getStatus())
                .entryDate(domain.getEntryDate())
                .exitDate(domain.getExitDate())
                .defectDescription(domain.getDefectDescription())
                .technicalResponsible(domain.getTechnicalResponsible())
                .verdictNotes(domain.getVerdictNotes())
                .originLoanId(domain.getOriginLoanId())
                .openedBy(domain.getOpenedBy())
                .closedBy(domain.getClosedBy())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
        List<MaintenanceCostJpaEntity> costs = new ArrayList<>();
        if (domain.getCosts() != null) {
            for (MaintenanceCost cost : domain.getCosts()) {
                costs.add(MaintenanceCostJpaEntity.builder()
                        .id(cost.getId())
                        .maintenance(entity)
                        .type(cost.getType())
                        .description(cost.getDescription())
                        .partReplaced(cost.getPartReplaced())
                        .amount(cost.getAmount())
                        .adjustment(cost.isAdjustment())
                        .adjustmentReason(cost.getAdjustmentReason())
                        .registeredBy(cost.getRegisteredBy())
                        .registeredAt(cost.getRegisteredAt())
                        .build());
            }
        }
        entity.setCosts(costs);
        return entity;
    }

    public Maintenance toDomainEntity(MaintenanceJpaEntity entity) {
        if (entity == null) return null;
        List<MaintenanceCost> costs = new ArrayList<>();
        if (entity.getCosts() != null) {
            for (MaintenanceCostJpaEntity cost : entity.getCosts()) {
                costs.add(MaintenanceCost.builder()
                        .id(cost.getId())
                        .maintenanceId(entity.getId())
                        .type(cost.getType())
                        .description(cost.getDescription())
                        .partReplaced(cost.getPartReplaced())
                        .amount(cost.getAmount())
                        .adjustment(cost.isAdjustment())
                        .adjustmentReason(cost.getAdjustmentReason())
                        .registeredBy(cost.getRegisteredBy())
                        .registeredAt(cost.getRegisteredAt())
                        .build());
            }
        }
        return Maintenance.builder()
                .id(entity.getId())
                .equipment(equipmentAdapter.toDomainEntity(entity.getEquipment()))
                .type(entity.getType())
                .status(entity.getStatus())
                .entryDate(entity.getEntryDate())
                .exitDate(entity.getExitDate())
                .defectDescription(entity.getDefectDescription())
                .technicalResponsible(entity.getTechnicalResponsible())
                .verdictNotes(entity.getVerdictNotes())
                .originLoanId(entity.getOriginLoanId())
                .openedBy(entity.getOpenedBy())
                .closedBy(entity.getClosedBy())
                .costs(costs)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
