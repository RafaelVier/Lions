package org.lions.backend.application.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lions.backend.application.service.audit.AuditService;
import org.lions.backend.application.support.AuditDetails;
import org.lions.backend.domain.entity.User;
import org.lions.backend.domain.entity.UserSession;
import org.lions.backend.domain.enums.AuditAction;
import org.lions.backend.domain.enums.SessionEndReason;
import org.lions.backend.domain.repository.UserRepositoryPort;
import org.lions.backend.domain.repository.UserSessionRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Sessões persistidas (RN57): cada token JWT ({@code jti}) tem uma sessão. Logout, renovação e inativação do usuário
 * encerram a sessão (o token passa a ser recusado, mesmo após reinício da aplicação); a rotina
 * {@code SessionExpirationJob} encerra as sessões que expiraram sem logout e audita {@code SESSION_EXPIRED}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    public static final String RULE = "RN57";

    private final UserSessionRepositoryPort sessionRepository;
    private final UserRepositoryPort userRepository;
    private final AuditService auditService;
    private final Clock clock;

    @Transactional
    public void start(String jti, String username, Instant expiresAt) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        sessionRepository.save(UserSession.builder()
                .jti(jti)
                .username(username)
                .issuedAt(LocalDateTime.now(clock))
                .expiresAt(LocalDateTime.ofInstant(expiresAt, clock.getZone()))
                .build());
    }

    /** Encerra a sessão do token. Retorna {@code true} se havia sessão aberta. */
    @Transactional
    public boolean end(String jti, SessionEndReason reason) {
        if (jti == null) {
            return false;
        }
        return sessionRepository.findByJti(jti)
                .filter(session -> session.end(reason, LocalDateTime.now(clock)))
                .map(sessionRepository::save)
                .isPresent();
    }

    /** Token recusado: a sessão dele foi encerrada (logout, renovação, inativação ou expiração). */
    @Transactional(readOnly = true)
    public boolean isRevoked(String jti) {
        return jti != null && sessionRepository.findByJti(jti).map(UserSession::isEnded).orElse(false);
    }

    /** Encerra todas as sessões abertas do usuário (ex.: inativação). Retorna quantas. */
    @Transactional
    public int revokeAll(String username) {
        LocalDateTime now = LocalDateTime.now(clock);
        int count = 0;
        for (UserSession session : sessionRepository.findOpenByUsername(username)) {
            if (session.end(SessionEndReason.REVOGADA, now)) {
                sessionRepository.save(session);
                count++;
            }
        }
        return count;
    }

    /** Rotina: encerra as sessões que expiraram sem logout e audita o encerramento (RN57, RN52). */
    @Transactional
    public int expireInactive() {
        LocalDateTime now = LocalDateTime.now(clock);
        int count = 0;
        for (UserSession session : sessionRepository.findOpenExpiredBefore(now)) {
            session.end(SessionEndReason.EXPIRADA, now);
            sessionRepository.save(session);
            Object entityId = userRepository.findByUsername(session.getUsername()).map(User::getId)
                    .map(Object.class::cast).orElse(session.getUsername());
            auditService.recordAutomatic(AuthService.ENTITY_NAME, entityId, AuditAction.SESSION_EXPIRED, RULE,
                    AuditDetails.of("username", session.getUsername(), "issuedAt", session.getIssuedAt(),
                            "expiredAt", session.getExpiresAt()));
            count++;
        }
        return count;
    }
}
