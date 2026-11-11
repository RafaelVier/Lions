package org.lions.backend.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.lions.backend.domain.entity.WaitlistEntry;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.WaitlistStatus;
import org.lions.backend.domain.repository.WaitlistEntryRepositoryPort;
import org.lions.backend.infrastructure.persistence.entity.WaitlistEntryJpaEntity;
import org.lions.backend.infrastructure.persistence.repository.BeneficiaryJpaRepository;
import org.lions.backend.infrastructure.persistence.repository.WaitlistEntryJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WaitlistEntryRepositoryAdapter implements WaitlistEntryRepositoryPort {

    private static final Set<WaitlistStatus> ACTIVE = EnumSet.of(WaitlistStatus.AGUARDANDO, WaitlistStatus.RESERVADO);

    private final WaitlistEntryJpaRepository jpaRepository;
    private final BeneficiaryJpaRepository beneficiaryJpaRepository;
    private final BeneficiaryRepositoryAdapter beneficiaryAdapter;

    @Override
    @Transactional
    public WaitlistEntry save(WaitlistEntry entry) {
        return toDomainEntity(jpaRepository.save(toJpaEntity(entry)));
    }

    @Override
    public Optional<WaitlistEntry> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomainEntity);
    }

    @Override
    public List<WaitlistEntry> findAll() {
        return map(jpaRepository.findAll());
    }

    @Override
    public List<WaitlistEntry> findActiveByType(EquipmentType type) {
        return map(jpaRepository.findByEquipmentTypeAndStatusOrderByPriorityDescQueuedAtAsc(type,
                WaitlistStatus.AGUARDANDO));
    }

    @Override
    public List<WaitlistEntry> findByStatus(WaitlistStatus status) {
        return map(jpaRepository.findByStatusOrderByQueuedAtAsc(status));
    }

    @Override
    public List<WaitlistEntry> findByBeneficiaryId(Long beneficiaryId) {
        return map(jpaRepository.findByBeneficiaryIdOrderByCreatedAtDesc(beneficiaryId));
    }

    @Override
    public List<WaitlistEntry> findActiveByBeneficiaryId(Long beneficiaryId) {
        return map(jpaRepository.findByBeneficiaryIdAndStatusIn(beneficiaryId, ACTIVE));
    }

    @Override
    public boolean existsActiveByBeneficiaryIdAndType(Long beneficiaryId, EquipmentType type) {
        return jpaRepository.existsByBeneficiaryIdAndEquipmentTypeAndStatusIn(beneficiaryId, type, ACTIVE);
    }

    @Override
    public List<WaitlistEntry> findExpiredReservations(LocalDateTime now) {
        return map(jpaRepository.findByStatusAndReservationExpiresAtBefore(WaitlistStatus.RESERVADO, now));
    }

    @Override
    public long countActiveByType(EquipmentType type) {
        return jpaRepository.countByEquipmentTypeAndStatus(type, WaitlistStatus.AGUARDANDO);
    }

    private List<WaitlistEntry> map(List<WaitlistEntryJpaEntity> entities) {
        return entities.stream().map(this::toDomainEntity).toList();
    }

    public WaitlistEntryJpaEntity toJpaEntity(WaitlistEntry domain) {
        if (domain == null) return null;
        Objects.requireNonNull(domain.getBeneficiary(), "Posição na fila sem beneficiário.");
        return WaitlistEntryJpaEntity.builder()
                .id(domain.getId())
                .beneficiary(beneficiaryJpaRepository.getReferenceById(domain.getBeneficiary().getId()))
                .equipmentType(domain.getEquipmentType())
                .orderId(domain.getOrderId())
                .status(domain.getStatus())
                .queuedAt(domain.getQueuedAt())
                .originalRequestedAt(domain.getOriginalRequestedAt())
                .priority(domain.isPriority())
                .priorityReason(domain.getPriorityReason())
                .priorityJustification(domain.getPriorityJustification())
                .priorityAuthorizedBy(domain.getPriorityAuthorizedBy())
                .reservedEquipmentId(domain.getReservedEquipmentId())
                .reservedAt(domain.getReservedAt())
                .reservationExpiresAt(domain.getReservationExpiresAt())
                .consecutiveExpirations(domain.getConsecutiveExpirations())
                .removedReason(domain.getRemovedReason())
                .closedAt(domain.getClosedAt())
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public WaitlistEntry toDomainEntity(WaitlistEntryJpaEntity entity) {
        if (entity == null) return null;
        return WaitlistEntry.builder()
                .id(entity.getId())
                .beneficiary(beneficiaryAdapter.toDomainEntity(entity.getBeneficiary()))
                .equipmentType(entity.getEquipmentType())
                .orderId(entity.getOrderId())
                .status(entity.getStatus())
                .queuedAt(entity.getQueuedAt())
                .originalRequestedAt(entity.getOriginalRequestedAt())
                .priority(entity.isPriority())
                .priorityReason(entity.getPriorityReason())
                .priorityJustification(entity.getPriorityJustification())
                .priorityAuthorizedBy(entity.getPriorityAuthorizedBy())
                .reservedEquipmentId(entity.getReservedEquipmentId())
                .reservedAt(entity.getReservedAt())
                .reservationExpiresAt(entity.getReservationExpiresAt())
                .consecutiveExpirations(entity.getConsecutiveExpirations())
                .removedReason(entity.getRemovedReason())
                .closedAt(entity.getClosedAt())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
