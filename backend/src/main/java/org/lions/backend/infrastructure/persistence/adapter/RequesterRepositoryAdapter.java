package org.lions.backend.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.lions.backend.domain.entity.Requester;
import org.lions.backend.domain.repository.RequesterRepositoryPort;
import org.lions.backend.infrastructure.persistence.entity.RequesterJpaEntity;
import org.lions.backend.infrastructure.persistence.repository.RequesterJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RequesterRepositoryAdapter implements RequesterRepositoryPort {

    private final RequesterJpaRepository jpaRepository;

    @Override
    public Requester save(Requester requester) {
        RequesterJpaEntity entity = toJpaEntity(requester);
        RequesterJpaEntity saved = jpaRepository.save(entity);
        return toDomainEntity(saved);
    }

    @Override
    public Optional<Requester> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomainEntity);
    }

    @Override
    public Optional<Requester> findByDocumentNumber(String documentNumber) {
        return jpaRepository.findByDocumentNumber(documentNumber).map(this::toDomainEntity);
    }

    @Override
    public List<Requester> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomainEntity).toList();
    }

    @Override
    public List<Requester> searchByName(String name) {
        return jpaRepository.searchByName(name).stream().map(this::toDomainEntity).toList();
    }

    public RequesterJpaEntity toJpaEntity(Requester domain) {
        if (domain == null) return null;
        return RequesterJpaEntity.builder()
                .id(domain.getId())
                .isForSelf(domain.isForSelf())
                .requesterName(domain.getRequesterName())
                .relationship(domain.getRelationship())
                .beneficiaryName(domain.getBeneficiaryName())
                .documentType(domain.getDocumentType())
                .documentNumber(domain.getDocumentNumber())
                .whatsapp(domain.getWhatsapp())
                .cep(domain.getCep())
                .street(domain.getStreet())
                .number(domain.getNumber())
                .neighborhood(domain.getNeighborhood())
                .city(domain.getCity())
                .state(domain.getState())
                .complement(domain.getComplement())
                .needReason(domain.getNeedReason())
                .consentGranted(domain.isConsentGranted())
                .consentTimestamp(domain.getConsentTimestamp())
                .assistedRegistration(domain.isAssistedRegistration())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public Requester toDomainEntity(RequesterJpaEntity entity) {
        if (entity == null) return null;
        return Requester.builder()
                .id(entity.getId())
                .isForSelf(entity.isForSelf())
                .requesterName(entity.getRequesterName())
                .relationship(entity.getRelationship())
                .beneficiaryName(entity.getBeneficiaryName())
                .documentType(entity.getDocumentType())
                .documentNumber(entity.getDocumentNumber())
                .whatsapp(entity.getWhatsapp())
                .cep(entity.getCep())
                .street(entity.getStreet())
                .number(entity.getNumber())
                .neighborhood(entity.getNeighborhood())
                .city(entity.getCity())
                .state(entity.getState())
                .complement(entity.getComplement())
                .needReason(entity.getNeedReason())
                .consentGranted(entity.isConsentGranted())
                .consentTimestamp(entity.getConsentTimestamp())
                .assistedRegistration(entity.isAssistedRegistration())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
