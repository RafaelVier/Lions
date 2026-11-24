package org.lions.backend.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.lions.backend.domain.entity.Beneficiary;
import org.lions.backend.domain.enums.NotificationChannel;
import org.lions.backend.domain.repository.BeneficiaryRepositoryPort;
import org.lions.backend.infrastructure.persistence.entity.AddressJpaEntity;
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
        BeneficiaryJpaEntity entity = toJpaEntity(beneficiary);
        if (entity.getAddress() != null && beneficiary.getId() != null) {
            // preserva a linha de tb_addresses (relação 1:1) em vez de recriá-la a cada edição
            jpaRepository.findById(beneficiary.getId())
                    .map(BeneficiaryJpaEntity::getAddress)
                    .ifPresent(existing -> entity.getAddress().setId(existing.getId()));
        }
        return toDomainEntity(jpaRepository.save(entity));
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
    public Optional<Beneficiary> findActiveByCpf(String cpf) {
        return jpaRepository.findFirstByCpfAndActiveTrue(cpf).map(this::toDomainEntity);
    }

    @Override
    public List<Beneficiary> searchByName(String name) {
        return jpaRepository.searchByName(name == null ? "" : name).stream().map(this::toDomainEntity).toList();
    }

    @Override
    public List<Beneficiary> findByCpf(String cpf) {
        return jpaRepository.findByCpfOrderByIdDesc(cpf).stream()
                .map(this::toDomainEntity).toList();
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public long countByActive(boolean active) {
        return jpaRepository.countByActive(active);
    }

    public BeneficiaryJpaEntity toJpaEntity(Beneficiary domain) {
        if (domain == null) return null;
        return BeneficiaryJpaEntity.builder()
                .id(domain.getId())
                .fullName(domain.getFullName())
                .cpf(domain.getCpf())
                .birthDate(domain.getBirthDate())
                .phone(domain.getPhone())
                .email(domain.getEmail())
                .address(toAddressEntity(domain))
                .alternativeContactName(domain.getAlternativeContactName())
                .alternativeContactPhone(domain.getAlternativeContactPhone())
                .alternativeContactRelationship(domain.getAlternativeContactRelationship())
                .legallyIncapable(domain.isLegallyIncapable())
                .legalGuardianName(domain.getLegalGuardianName())
                .legalGuardianCpf(domain.getLegalGuardianCpf())
                .legalGuardianPhone(domain.getLegalGuardianPhone())
                .legalGuardianEmail(domain.getLegalGuardianEmail())
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
        AddressJpaEntity address = entity.getAddress();
        return Beneficiary.builder()
                .id(entity.getId())
                .fullName(entity.getFullName())
                .cpf(entity.getCpf())
                .birthDate(entity.getBirthDate())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .cep(address != null ? address.getCep() : null)
                .street(address != null ? address.getStreet() : null)
                .number(address != null ? address.getNumber() : null)
                .complement(address != null ? address.getComplement() : null)
                .neighborhood(address != null ? address.getNeighborhood() : null)
                .city(address != null ? address.getCity() : null)
                .state(address != null ? address.getState() : null)
                .alternativeContactName(entity.getAlternativeContactName())
                .alternativeContactPhone(entity.getAlternativeContactPhone())
                .alternativeContactRelationship(entity.getAlternativeContactRelationship())
                .legallyIncapable(entity.isLegallyIncapable())
                .legalGuardianName(entity.getLegalGuardianName())
                .legalGuardianCpf(entity.getLegalGuardianCpf())
                .legalGuardianPhone(entity.getLegalGuardianPhone())
                .legalGuardianEmail(entity.getLegalGuardianEmail())
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

    /** Endereço em tabela própria (MER: tb_endereco); sem nenhum campo preenchido (ex.: anonimizado) não há linha. */
    private static AddressJpaEntity toAddressEntity(Beneficiary domain) {
        if (domain.getCep() == null && domain.getStreet() == null && domain.getNumber() == null
                && domain.getComplement() == null && domain.getNeighborhood() == null
                && domain.getCity() == null && domain.getState() == null) {
            return null;
        }
        return AddressJpaEntity.builder()
                .cep(domain.getCep())
                .street(domain.getStreet())
                .number(domain.getNumber())
                .complement(domain.getComplement())
                .neighborhood(domain.getNeighborhood())
                .city(domain.getCity())
                .state(domain.getState())
                .build();
    }
}
