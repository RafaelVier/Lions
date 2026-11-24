package org.lions.backend.infrastructure.persistence.repository;

import org.lions.backend.domain.enums.HelpAudience;
import org.lions.backend.infrastructure.persistence.entity.HelpContentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HelpContentJpaRepository extends JpaRepository<HelpContentJpaEntity, Long> {
    @Query("SELECT DISTINCT h FROM HelpContentJpaEntity h JOIN h.audiences a "
            + "WHERE h.active = true AND a = :audience ORDER BY h.displayOrder, h.id")
    List<HelpContentJpaEntity> findActiveByAudience(@Param("audience") HelpAudience audience);
}
