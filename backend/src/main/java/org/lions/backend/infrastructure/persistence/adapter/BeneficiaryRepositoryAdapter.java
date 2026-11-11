package org.lions.backend.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.lions.backend.domain.entity.Beneficiary;
import org.lions.backend.domain.enums.NotificationChannel;
import org.lions.backend.domain.repository.BeneficiaryRepositoryPort;
import org.lions.backend.infrastructure.persistence.entity.BeneficiaryJpaEntity;
import org.lions.backend.infrastructure.persistence.repository.BeneficiaryJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BeneficiaryRepositoryAdapter implements BeneficiaryRepositoryPort {

    private final BeneficiaryJpaRepository jpaRepository;

    @Override
    @Transactional
    public Beneficiary save(Beneficiary beneficiary) {
        return toDomainEntity(jpaRepository.save(toJpaEntity(beneficiary)));
    }

    @Override
    public Optional<Beneficiary> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomainEntity);
    }

    @Override
    public List<Beneficiary> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomainEntity).toList();
    }

    @Override
    public List<Beneficiary> findAll(Boolean active) {
        if (active == null) {
            return findAll();
        }
        return jpaRepository.findByActive(active).stream().map(this::toDomainEntity).toList();
    }

    @Override
    public Optional<Beneficiary> findActiveByDocumentNumber(String documentNumber) {
        return jpaRepository.findFirstByDocumentNumberAndActiveTrue(documentNumber).map(this::toDomainEntity);
    }

    @Override
    public List<Beneficiary> searchByName(String name) {
        return jpaRepository.searchByName(name == null ? "" : name).stream().map(this::toDomainEntity).toList();
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    public BeneficiaryJpaEntity toJpaEntity(Beneficiary domain) {
        if (domain == null) return null;
        return BeneficiaryJpaEntity.builder()
                .id(domain.getId())
                .fullName(domain.getFullName())
                .documentType(domain.getDocumentType())
                .documentNumber(domain.getDocumentNumber())
                .birthDate(domain.getBirthDate())
                .phone(domain.getPhone())
                .email(domain.getEmail())
                .cep(domain.getCep())
                .street(domain.getStreet())
                .number(domain.getNumber())
                .complement(domain.getComplement())
                .neighborhood(domain.getNeighborhood())
                .city(domain.getCity())
                .state(domain.getState())
                .alternativeContactName(domain.getAlternativeContactName())
                .alternativeContactPhone(domain.getAlternativeContactPhone())
                .alternativeContactRelationship(domain.getAlternativeContactRelationship())
                .legallyIncapable(domain.isLegallyIncapable())
                .legalGuardianName(domain.getLegalGuardianName())
                .legalGuardianDocument(domain.getLegalGuardianDocument())
                .legalGuardianPhone(domain.getLegalGuardianPhone())
                .legalGuardianRelationship(domain.getLegalGuardianRelationship())
                .consentGranted(domain.isConsentGranted())
                .consentTimestamp(domain.getConsentTimestamp())
                .consentGivenBy(domain.getConsentGivenBy())
                .notificationChannels(domain.getNotificationChannels() == null
                        ? new HashSet<>()
                        : new HashSet<>(domain.getNotificationChannels()))
                .active(domain.isActive())
                .inactivatedAt(domain.getInactivatedAt())
                .inactivationReason(domain.getInactivationReason())
                .anonymized(domain.isAnonymized())
                .anonymizedAt(domain.getAnonymizedAt())
                .notes(domain.getNotes())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public Beneficiary toDomainEntity(BeneficiaryJpaEntity entity) {
        if (entity == null) return null;
        Set<NotificationChannel> channels = EnumSet.noneOf(NotificationChannel.class);
        if (entity.getNotificationChannels() != null) {
            channels.addAll(entity.getNotificationChannels());
        }
        return Beneficiary.builder()
                .id(entity.getId())
                .fullName(entity.getFullName())
                .documentType(entity.getDocumentType())
                .documentNumber(entity.getDocumentNumber())
                .birthDate(entity.getBirthDate())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .cep(entity.getCep())
                .street(entity.getStreet())
                .number(entity.getNumber())
                .complement(entity.getComplement())
                .neighborhood(entity.getNeighborhood())
                .city(entity.getCity())
                .state(entity.getState())
                .alternativeContactName(entity.getAlternativeContactName())
                .alternativeContactPhone(entity.getAlternativeContactPhone())
                .alternativeContactRelationship(entity.getAlternativeContactRelationship())
                .legallyIncapable(entity.isLegallyIncapable())
                .legalGuardianName(entity.getLegalGuardianName())
                .legalGuardianDocument(entity.getLegalGuardianDocument())
                .legalGuardianPhone(entity.getLegalGuardianPhone())
                .legalGuardianRelationship(entity.getLegalGuardianRelationship())
                .consentGranted(entity.isConsentGranted())
                .consentTimestamp(entity.getConsentTimestamp())
                .consentGivenBy(entity.getConsentGivenBy())
                .notificationChannels(channels)
                .active(entity.isActive())
                .inactivatedAt(entity.getInactivatedAt())
                .inactivationReason(entity.getInactivationReason())
                .anonymized(entity.isAnonymized())
                .anonymizedAt(entity.getAnonymizedAt())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
