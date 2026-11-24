package org.lions.backend.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.lions.backend.domain.entity.UserSession;
import org.lions.backend.domain.repository.UserSessionRepositoryPort;
import org.lions.backend.infrastructure.persistence.entity.UserSessionJpaEntity;
import org.lions.backend.infrastructure.persistence.repository.UserSessionJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSessionRepositoryAdapter implements UserSessionRepositoryPort {

    private final UserSessionJpaRepository jpaRepository;

    @Override
    @Transactional
    public UserSession save(UserSession session) {
        return toDomain(jpaRepository.save(UserSessionJpaEntity.builder()
                .id(session.getId())
                .jti(session.getJti())
                .username(session.getUsername())
                .issuedAt(session.getIssuedAt())
                .expiresAt(session.getExpiresAt())
                .endedAt(session.getEndedAt())
                .endReason(session.getEndReason())
                .build()));
    }

    @Override
    public Optional<UserSession> findByJti(String jti) {
        return jpaRepository.findByJti(jti).map(this::toDomain);
    }

    @Override
    public List<UserSession> findOpenExpiredBefore(LocalDateTime now) {
        return jpaRepository.findByEndedAtIsNullAndExpiresAtLessThanEqual(now).stream().map(this::toDomain).toList();
    }

    @Override
    public List<UserSession> findOpenByUsername(String username) {
        return jpaRepository.findByUsernameAndEndedAtIsNull(username).stream().map(this::toDomain).toList();
    }

    private UserSession toDomain(UserSessionJpaEntity e) {
        return UserSession.builder()
                .id(e.getId())
                .jti(e.getJti())
                .username(e.getUsername())
                .issuedAt(e.getIssuedAt())
                .expiresAt(e.getExpiresAt())
                .endedAt(e.getEndedAt())
                .endReason(e.getEndReason())
                .build();
    }
}
