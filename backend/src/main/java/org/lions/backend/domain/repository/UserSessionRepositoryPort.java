package org.lions.backend.domain.repository;

import org.lions.backend.domain.entity.UserSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserSessionRepositoryPort {
    UserSession save(UserSession session);

    Optional<UserSession> findByJti(String jti);

    /** Sessões não encerradas com expiração até {@code now}. */
    List<UserSession> findOpenExpiredBefore(LocalDateTime now);

    /** Sessões não encerradas do usuário. */
    List<UserSession> findOpenByUsername(String username);
}
