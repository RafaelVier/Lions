package org.lions.backend.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.lions.backend.domain.entity.HelpContent;
import org.lions.backend.domain.enums.HelpAudience;
import org.lions.backend.domain.repository.HelpContentRepositoryPort;
import org.lions.backend.infrastructure.persistence.entity.HelpContentJpaEntity;
import org.lions.backend.infrastructure.persistence.repository.HelpContentJpaRepository;
import org.springframework.data.domain.Sort;
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
public class HelpContentRepositoryAdapter implements HelpContentRepositoryPort {

    private final HelpContentJpaRepository jpaRepository;

    @Override
    public List<HelpContent> findActiveByAudience(HelpAudience audience) {
        return jpaRepository.findActiveByAudience(audience).stream().map(this::toDomainEntity).toList();
    }

    @Override
    public List<HelpContent> findAll() {
        return jpaRepository.findAll(Sort.by("displayOrder", "id")).stream().map(this::toDomainEntity).toList();
    }

    @Override
    public Optional<HelpContent> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomainEntity);
    }

    @Override
    @Transactional
    public HelpContent save(HelpContent content) {
        return toDomainEntity(jpaRepository.save(toJpaEntity(content)));
    }

    public HelpContentJpaEntity toJpaEntity(HelpContent domain) {
        if (domain == null) return null;
        return HelpContentJpaEntity.builder()
                .id(domain.getId())
                .type(domain.getType())
                .title(domain.getTitle())
                .content(domain.getContent())
                .keywords(domain.getKeywords())
                .audiences(domain.getAudiences() == null ? new HashSet<>() : new HashSet<>(domain.getAudiences()))
                .displayOrder(domain.getDisplayOrder())
                .active(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public HelpContent toDomainEntity(HelpContentJpaEntity entity) {
        if (entity == null) return null;
        Set<HelpAudience> audiences = EnumSet.noneOf(HelpAudience.class);
        if (entity.getAudiences() != null) {
            audiences.addAll(entity.getAudiences());
        }
        return HelpContent.builder()
                .id(entity.getId())
                .type(entity.getType())
                .title(entity.getTitle())
                .content(entity.getContent())
                .keywords(entity.getKeywords())
                .audiences(audiences)
                .displayOrder(entity.getDisplayOrder())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
