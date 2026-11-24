package org.lions.backend.domain.repository;

import org.lions.backend.domain.entity.HelpContent;
import org.lions.backend.domain.enums.HelpAudience;

import java.util.List;
import java.util.Optional;

public interface HelpContentRepositoryPort {
    /** Ativos visíveis para o público, ordenados por {@code displayOrder}. */
    List<HelpContent> findActiveByAudience(HelpAudience audience);

    List<HelpContent> findAll();

    Optional<HelpContent> findById(Long id);

    HelpContent save(HelpContent content);
}
