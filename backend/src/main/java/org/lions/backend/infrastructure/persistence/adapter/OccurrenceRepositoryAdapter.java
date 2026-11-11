package org.lions.backend.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.lions.backend.domain.entity.Occurrence;
import org.lions.backend.domain.enums.OccurrenceStatus;
import org.lions.backend.domain.enums.OccurrenceType;
import org.lions.backend.domain.repository.OccurrenceRepositoryPort;
import org.lions.backend.infrastructure.persistence.entity.OccurrenceJpaEntity;
import org.lions.backend.infrastructure.persistence.repository.BeneficiaryJpaRepository;
import org.lions.backend.infrastructure.persistence.repository.OccurrenceJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OccurrenceRepositoryAdapter implements OccurrenceRepositoryPort {

    private final OccurrenceJpaRepository jpaRepository;
    private final BeneficiaryJpaRepository beneficiaryJpaRepository;
    private final BeneficiaryRepositoryAdapter beneficiaryAdapter;

    @Override
    @Transactional
    public Occurrence save(Occurrence occurrence) {
        return toDomainEntity(jpaRepository.save(toJpaEntity(occurrence)));
    }

    @Override
    public Optional<Occurrence> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomainEntity);
    }

    @Override
    public List<Occurrence> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomainEntity).toList();
    }

    @Override
    public List<Occurrence> findByBeneficiaryId(Long beneficiaryId) {
        return jpaRepository.findByBeneficiaryIdOrderByCreatedAtDesc(beneficiaryId).stream()
                .map(this::toDomainEntity).toList();
    }

    @Override
    public List<Occurrence> findByStatus(OccurrenceStatus status) {
        return jpaRepository.findByStatusOrderByCreatedAtDesc(status).stream().map(this::toDomainEntity).toList();
    }

    @Override
    public boolean existsByBeneficiaryIdAndStatus(Long beneficiaryId, OccurrenceStatus status) {
        return jpaRepository.existsByBeneficiaryIdAndStatus(beneficiaryId, status);
    }

    @Override
    public boolean existsByLoanIdAndType(Long loanId, OccurrenceType type) {
        return jpaRepository.existsByLoanIdAndType(loanId, type);
    }

    public OccurrenceJpaEntity toJpaEntity(Occurrence domain) {
        if (domain == null) return null;
        Objects.requireNonNull(domain.getBeneficiary(), "Ocorrência sem beneficiário.");
        return OccurrenceJpaEntity.builder()
                .id(domain.getId())
                .beneficiary(beneficiaryJpaRepository.getReferenceById(domain.getBeneficiary().getId()))
                .loanId(domain.getLoanId())
                .type(domain.getType())
                .status(domain.getStatus())
                .description(domain.getDescription())
                .automatic(domain.isAutomatic())
                .createdBy(domain.getCreatedBy())
                .resolvedAt(domain.getResolvedAt())
                .resolvedBy(domain.getResolvedBy())
                .resolutionNote(domain.getResolutionNote())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public Occurrence toDomainEntity(OccurrenceJpaEntity entity) {
        if (entity == null) return null;
        return Occurrence.builder()
                .id(entity.getId())
                .beneficiary(beneficiaryAdapter.toDomainEntity(entity.getBeneficiary()))
                .loanId(entity.getLoanId())
                .type(entity.getType())
                .status(entity.getStatus())
                .description(entity.getDescription())
                .automatic(entity.isAutomatic())
                .createdBy(entity.getCreatedBy())
                .resolvedAt(entity.getResolvedAt())
                .resolvedBy(entity.getResolvedBy())
                .resolutionNote(entity.getResolutionNote())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
