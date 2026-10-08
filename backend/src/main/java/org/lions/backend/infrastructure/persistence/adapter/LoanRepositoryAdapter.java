package org.lions.backend.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.lions.backend.domain.entity.Loan;
import org.lions.backend.domain.enums.LoanStatus;
import org.lions.backend.domain.repository.LoanRepositoryPort;
import org.lions.backend.infrastructure.persistence.entity.LoanJpaEntity;
import org.lions.backend.infrastructure.persistence.repository.LoanJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LoanRepositoryAdapter implements LoanRepositoryPort {

    private final LoanJpaRepository jpaRepository;
    private final LoanOrderRepositoryAdapter orderAdapter;
    private final EquipmentRepositoryAdapter equipmentAdapter;

    @Override
    public Loan save(Loan loan) {
        LoanJpaEntity entity = toJpaEntity(loan);
        LoanJpaEntity saved = jpaRepository.save(entity);
        return toDomainEntity(saved);
    }

    @Override
    public Optional<Loan> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomainEntity);
    }

    @Override
    public List<Loan> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomainEntity).toList();
    }

    @Override
    public List<Loan> findByStatus(LoanStatus status) {
        return jpaRepository.findByStatus(status).stream().map(this::toDomainEntity).toList();
    }

    @Override
    public List<Loan> findDueBetween(LocalDate start, LocalDate end) {
        return jpaRepository.findDueBetween(start, end).stream().map(this::toDomainEntity).toList();
    }

    @Override
    public List<Loan> findOverdue(LocalDate cutoffDate) {
        return jpaRepository.findOverdue(cutoffDate).stream().map(this::toDomainEntity).toList();
    }

    @Override
    public List<Loan> findByEquipmentId(Long equipmentId) {
        return jpaRepository.findByEquipmentId(equipmentId).stream().map(this::toDomainEntity).toList();
    }

    @Override
    public List<Loan> findByRequesterId(Long requesterId) {
        return jpaRepository.findByRequesterId(requesterId).stream().map(this::toDomainEntity).toList();
    }

    @Override
    public long countByStatus(LoanStatus status) {
        return jpaRepository.countByStatus(status);
    }

    public LoanJpaEntity toJpaEntity(Loan domain) {
        if (domain == null) return null;
        return LoanJpaEntity.builder()
                .id(domain.getId())
                .order(orderAdapter.toJpaEntity(domain.getOrder()))
                .equipment(equipmentAdapter.toJpaEntity(domain.getEquipment()))
                .loanDate(domain.getLoanDate())
                .initialExpectedReturnDate(domain.getInitialExpectedReturnDate())
                .currentDueDate(domain.getCurrentDueDate())
                .status(domain.getStatus())
                .initialCondition(domain.getInitialCondition())
                .deliveryPhotoUrl(domain.getDeliveryPhotoUrl())
                .deliveryNotes(domain.getDeliveryNotes())
                .renewalCount(domain.getRenewalCount())
                .lastRenewalDate(domain.getLastRenewalDate())
                .termPdfPath(domain.getTermPdfPath())
                .termHashSha256(domain.getTermHashSha256())
                .electronicAcceptanceDate(domain.getElectronicAcceptanceDate())
                .acceptedByName(domain.getAcceptedByName())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public Loan toDomainEntity(LoanJpaEntity entity) {
        if (entity == null) return null;
        return Loan.builder()
                .id(entity.getId())
                .order(orderAdapter.toDomainEntity(entity.getOrder()))
                .equipment(equipmentAdapter.toDomainEntity(entity.getEquipment()))
                .loanDate(entity.getLoanDate())
                .initialExpectedReturnDate(entity.getInitialExpectedReturnDate())
                .currentDueDate(entity.getCurrentDueDate())
                .status(entity.getStatus())
                .initialCondition(entity.getInitialCondition())
                .deliveryPhotoUrl(entity.getDeliveryPhotoUrl())
                .deliveryNotes(entity.getDeliveryNotes())
                .renewalCount(entity.getRenewalCount())
                .lastRenewalDate(entity.getLastRenewalDate())
                .termPdfPath(entity.getTermPdfPath())
                .termHashSha256(entity.getTermHashSha256())
                .electronicAcceptanceDate(entity.getElectronicAcceptanceDate())
                .acceptedByName(entity.getAcceptedByName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
