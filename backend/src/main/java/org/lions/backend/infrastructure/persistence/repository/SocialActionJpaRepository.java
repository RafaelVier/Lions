package org.lions.backend.infrastructure.persistence.repository;

import org.lions.backend.domain.enums.GlobalCause;
import org.lions.backend.infrastructure.persistence.entity.SocialActionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SocialActionJpaRepository extends JpaRepository<SocialActionJpaEntity, Long> {
    List<SocialActionJpaEntity> findByClubId(Long clubId);
    List<SocialActionJpaEntity> findByCause(GlobalCause cause);
}
