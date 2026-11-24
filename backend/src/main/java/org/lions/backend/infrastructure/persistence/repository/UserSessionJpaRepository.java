package org.lions.backend.infrastructure.persistence.repository;

import org.lions.backend.infrastructure.persistence.entity.UserSessionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionJpaRepository extends JpaRepository<UserSessionJpaEntity, Long> {
    Optional<UserSessionJpaEntity> findByJti(String jti);

    List<UserSessionJpaEntity> findByEndedAtIsNullAndExpiresAtLessThanEqual(LocalDateTime now);

    List<UserSessionJpaEntity> findByUsernameAndEndedAtIsNull(String username);
}
