package org.lions.backend.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.lions.backend.domain.entity.LionsClub;
import org.lions.backend.domain.repository.LionsClubRepositoryPort;
import org.lions.backend.infrastructure.persistence.entity.LionsClubJpaEntity;
import org.lions.backend.infrastructure.persistence.repository.LionsClubJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LionsClubRepositoryAdapter implements LionsClubRepositoryPort {

    private final LionsClubJpaRepository jpaRepository;

    @Override
    public LionsClub save(LionsClub club) {
        LionsClubJpaEntity entity = toJpaEntity(club);
        LionsClubJpaEntity saved = jpaRepository.save(entity);
        return toDomainEntity(saved);
    }

    @Override
    public Optional<LionsClub> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomainEntity);
    }

    @Override
    public List<LionsClub> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomainEntity).toList();
    }

    @Override
    public List<LionsClub> findByDistrict(String district) {
        return jpaRepository.findByDistrict(district).stream().map(this::toDomainEntity).toList();
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private LionsClubJpaEntity toJpaEntity(LionsClub domain) {
        return LionsClubJpaEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .clubNumber(domain.getClubNumber())
                .district(domain.getDistrict())
                .region(domain.getRegion())
                .city(domain.getCity())
                .state(domain.getState())
                .zipCode(domain.getZipCode())
                .foundationDate(domain.getFoundationDate())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private LionsClub toDomainEntity(LionsClubJpaEntity entity) {
        return LionsClub.builder()
                .id(entity.getId())
                .name(entity.getName())
                .clubNumber(entity.getClubNumber())
                .district(entity.getDistrict())
                .region(entity.getRegion())
                .city(entity.getCity())
                .state(entity.getState())
                .zipCode(entity.getZipCode())
                .foundationDate(entity.getFoundationDate())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
