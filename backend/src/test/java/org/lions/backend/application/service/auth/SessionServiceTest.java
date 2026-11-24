package org.lions.backend.application.service.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lions.backend.application.service.audit.AuditService;
import org.lions.backend.domain.entity.User;
import org.lions.backend.domain.entity.UserSession;
import org.lions.backend.domain.enums.AuditAction;
import org.lions.backend.domain.enums.SessionEndReason;
import org.lions.backend.domain.repository.UserRepositoryPort;
import org.lions.backend.domain.repository.UserSessionRepositoryPort;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionServiceTest {

    private static final Instant T0 = Instant.parse("2025-03-10T12:00:00Z");

    /** Relógio ajustável. */
    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final Map<String, UserSession> store = new LinkedHashMap<>();
    private final AtomicLong ids = new AtomicLong();
    private MutableClock clock;
    private AuditService auditService;
    private SessionService service;

    @BeforeEach
    void setUp() {
        UserSessionRepositoryPort repository = new UserSessionRepositoryPort() {
            @Override
            public UserSession save(UserSession session) {
                if (session.getId() == null) {
                    session.setId(ids.incrementAndGet());
                }
                store.put(session.getJti(), session);
                return session;
            }

            @Override
            public Optional<UserSession> findByJti(String jti) {
                return Optional.ofNullable(store.get(jti));
            }

            @Override
            public List<UserSession> findOpenExpiredBefore(LocalDateTime now) {
                return new ArrayList<>(store.values().stream()
                        .filter(s -> !s.isEnded() && !s.getExpiresAt().isAfter(now)).toList());
            }

            @Override
            public List<UserSession> findOpenByUsername(String username) {
                return new ArrayList<>(store.values().stream()
                        .filter(s -> !s.isEnded() && s.getUsername().equals(username)).toList());
            }
        };
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        when(users.findByUsername("marcio")).thenReturn(Optional.of(User.builder().id(7L).username("marcio").build()));
        auditService = mock(AuditService.class);
        clock = new MutableClock(T0);
        service = new SessionService(repository, users, auditService, clock);
    }

    @Test
    void deveRecusarTokenQuandoSessaoEncerrada() {
        service.start("jti-1", "marcio", T0.plus(Duration.ofMinutes(30)));
        assertThat(service.isRevoked("jti-1")).isFalse();
        assertThat(service.isRevoked("desconhecido")).isFalse();
        assertThat(service.isRevoked(null)).isFalse();

        assertThat(service.end("jti-1", SessionEndReason.LOGOUT)).isTrue();
        assertThat(service.end("jti-1", SessionEndReason.LOGOUT)).isFalse();
        assertThat(service.isRevoked("jti-1")).isTrue();
        assertThat(store.get("jti-1").getEndReason()).isEqualTo(SessionEndReason.LOGOUT);
    }

    @Test
    void deveExpirarEAuditarQuandoSessaoVenceuSemLogout() {
        service.start("ativo", "marcio", T0.plus(Duration.ofMinutes(60)));
        service.start("vencido", "marcio", T0.plus(Duration.ofMinutes(30)));
        service.start("saiu", "marcio", T0.plus(Duration.ofMinutes(30)));
        service.end("saiu", SessionEndReason.LOGOUT);

        clock.advance(Duration.ofMinutes(31));
        assertThat(service.expireInactive()).isEqualTo(1);

        assertThat(store.get("vencido").getEndReason()).isEqualTo(SessionEndReason.EXPIRADA);
        assertThat(store.get("ativo").isEnded()).isFalse();
        assertThat(store.get("saiu").getEndReason()).isEqualTo(SessionEndReason.LOGOUT);
        verify(auditService, times(1)).recordAutomatic(eq(AuthService.ENTITY_NAME), eq(7L),
                eq(AuditAction.SESSION_EXPIRED), eq(SessionService.RULE), any());
        assertThat(service.expireInactive()).isZero();
    }

    @Test
    void deveRevogarTodasAsSessoesQuandoUsuarioInativado() {
        service.start("a", "marcio", T0.plus(Duration.ofMinutes(30)));
        service.start("b", "marcio", T0.plus(Duration.ofMinutes(30)));
        service.start("c", "outra", T0.plus(Duration.ofMinutes(30)));

        assertThat(service.revokeAll("marcio")).isEqualTo(2);
        assertThat(service.isRevoked("a")).isTrue();
        assertThat(service.isRevoked("c")).isFalse();
        verify(auditService, never()).recordAutomatic(any(), any(), any(), any(), any());
    }
}
