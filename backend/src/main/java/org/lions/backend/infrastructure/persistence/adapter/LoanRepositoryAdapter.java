package org.lions.backend.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.lions.backend.domain.entity.Loan;
import org.lions.backend.domain.enums.LoanStatus;
import org.lions.backend.domain.repository.LoanRepositoryPort;
import org.lions.backend.infrastructure.persistence.entity.LoanJpaEntity;
import org.lions.backend.infrastructure.persistence.repository.BeneficiaryJpaRepository;
import org.lions.backend.infrastructure.persistence.repository.EquipmentJpaRepository;
import org.lions.backend.infrastructure.persistence.repository.LoanJpaRepository;
import org.lions.backend.infrastructure.persistence.repository.LoanOrderJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanRepositoryAdapter implements LoanRepositoryPort {

    private final LoanJpaRepository jpaRepository;
    private final BeneficiaryJpaRepository beneficiaryJpaRepository;
    private final EquipmentJpaRepository equipmentJpaRepository;
    private final LoanOrderJpaRepository orderJpaRepository;
    private final BeneficiaryRepositoryAdapter beneficiaryAdapter;
    private final EquipmentRepositoryAdapter equipmentAdapter;
    private final LoanOrderRepositoryAdapter orderAdapter;

    @Override
    @Transactional
    public Loan save(Loan loan) {
        return toDomainEntity(jpaRepository.save(toJpaEntity(loan)));
    }

    @Override
    public Optional<Loan> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomainEntity);
    }

    @Override
    public List<Loan> findAll() {
        return map(jpaRepository.findAll());
    }

    @Override
    public List<Loan> findByStatus(LoanStatus status) {
        return map(jpaRepository.findByStatus(status));
    }

    @Override
    public List<Loan> findByStatusIn(Collection<LoanStatus> statuses) {
        return map(jpaRepository.findByStatusIn(statuses));
    }

    @Override
    public List<Loan> findByBeneficiaryId(Long beneficiaryId) {
        return map(jpaRepository.findByBeneficiaryIdOrderByLoanDateDesc(beneficiaryId));
    }

    @Override
    public List<Loan> findByEquipmentId(Long equipmentId) {
        return map(jpaRepository.findByEquipmentIdOrderByLoanDateDesc(equipmentId));
    }

    @Override
    public List<Loan> findOpenByBeneficiaryId(Long beneficiaryId) {
        return map(jpaRepository.findByBeneficiaryIdAndStatusIn(beneficiaryId, LoanStatus.openStatuses()));
    }

    @Override
    public List<Loan> findOpenDueBetween(LocalDate start, LocalDate end) {
        return map(jpaRepository.findByStatusInAndDueBetween(LoanStatus.openStatuses(), start, end));
    }

    @Override
    public List<Loan> findOpenOverdue(LocalDate today) {
        return map(jpaRepository.findByStatusInAndDueBefore(LoanStatus.openStatuses(), today));
    }

    @Override
    public List<Loan> findByLoanDateBetween(LocalDate start, LocalDate end) {
        return map(jpaRepository.findByLoanDateBetweenOrderByLoanDate(start, end));
    }

    @Override
    public long countByStatus(LoanStatus status) {
        return jpaRepository.countByStatus(status);
    }

    @Override
    public long countByLoanDateBetween(LocalDate start, LocalDate end) {
        return jpaRepository.countByLoanDateBetween(start, end);
    }

    private List<Loan> map(List<LoanJpaEntity> entities) {
        return entities.stream().map(this::toDomainEntity).toList();
    }

    public LoanJpaEntity toJpaEntity(Loan domain) {
        if (domain == null) return null;
        Objects.requireNonNull(domain.getBeneficiary(), "Empréstimo sem beneficiário.");
        Objects.requireNonNull(domain.getEquipment(), "Empréstimo sem equipamento.");
        return LoanJpaEntity.builder()
                .id(domain.getId())
                .beneficiary(beneficiaryJpaRepository.getReferenceById(domain.getBeneficiary().getId()))
                .equipment(equipmentJpaRepository.getReferenceById(domain.getEquipment().getId()))
                .order(domain.getOrder() == null || domain.getOrder().getId() == null
                        ? null
                        : orderJpaRepository.getReferenceById(domain.getOrder().getId()))
                .status(domain.getStatus())
                .loanDate(domain.getLoanDate())
                .initialExpectedReturnDate(domain.getInitialExpectedReturnDate())
                .expectedReturnDate(domain.getExpectedReturnDate())
                .renewalCount(domain.getRenewalCount())
                .lastRenewalDate(domain.getLastRenewalDate())
                .retroactive(domain.isRetroactive())
                .retroactiveJustification(domain.getRetroactiveJustification())
                .initialCondition(domain.getInitialCondition())
                .deliveryNotes(domain.getDeliveryNotes())
                .deliveryPhotoUrl(domain.getDeliveryPhotoUrl())
                .termPdfPath(domain.getTermPdfPath())
                .termHashSha256(domain.getTermHashSha256())
                .termGeneratedAt(domain.getTermGeneratedAt())
                .signatureType(domain.getSignatureType())
                .signedAt(domain.getSignedAt())
                .signedByName(domain.getSignedByName())
                .signedDocumentPath(domain.getSignedDocumentPath())
                .signatureRegisteredBy(domain.getSignatureRegisteredBy())
                .createdBy(domain.getCreatedBy())
                .returnedAt(domain.getReturnedAt())
                .cancelReason(domain.getCancelReason())
                .canceledBy(domain.getCanceledBy())
                .canceledAt(domain.getCanceledAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public Loan toDomainEntity(LoanJpaEntity entity) {
        if (entity == null) return null;
        return Loan.builder()
                .id(entity.getId())
                .beneficiary(beneficiaryAdapter.toDomainEntity(entity.getBeneficiary()))
                .equipment(equipmentAdapter.toDomainEntity(entity.getEquipment()))
                .order(orderAdapter.toDomainEntity(entity.getOrder()))
                .status(entity.getStatus())
                .loanDate(entity.getLoanDate())
                .initialExpectedReturnDate(entity.getInitialExpectedReturnDate())
                .expectedReturnDate(entity.getExpectedReturnDate())
                .renewalCount(entity.getRenewalCount())
                .lastRenewalDate(entity.getLastRenewalDate())
                .retroactive(entity.isRetroactive())
                .retroactiveJustification(entity.getRetroactiveJustification())
                .initialCondition(entity.getInitialCondition())
                .deliveryNotes(entity.getDeliveryNotes())
                .deliveryPhotoUrl(entity.getDeliveryPhotoUrl())
                .termPdfPath(entity.getTermPdfPath())
                .termHashSha256(entity.getTermHashSha256())
                .termGeneratedAt(entity.getTermGeneratedAt())
                .signatureType(entity.getSignatureType())
                .signedAt(entity.getSignedAt())
                .signedByName(entity.getSignedByName())
                .signedDocumentPath(entity.getSignedDocumentPath())
                .signatureRegisteredBy(entity.getSignatureRegisteredBy())
                .createdBy(entity.getCreatedBy())
                .returnedAt(entity.getReturnedAt())
                .cancelReason(entity.getCancelReason())
                .canceledBy(entity.getCanceledBy())
                .canceledAt(entity.getCanceledAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
