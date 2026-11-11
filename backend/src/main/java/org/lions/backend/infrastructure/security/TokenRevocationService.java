package org.lions.backend.infrastructure.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lista de revogação de tokens JWT em memória ({@code jti → expiração}), usada no logout e na
 * desativação de usuários. Entradas expiradas são descartadas (o próprio token já seria rejeitado).
 * Em memória: reinício da aplicação limpa a lista (aceitável com tokens de 30 min).
 */
@Component
public class TokenRevocationService {

    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();
    private final Clock clock;

    public TokenRevocationService(Clock clock) {
        this.clock = clock;
    }

    public void revoke(String jti, Instant expiresAt) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        purgeExpired();
        revoked.put(jti, expiresAt != null ? expiresAt : Instant.MAX);
    }

    public boolean isRevoked(String jti) {
        if (jti == null) {
            return false;
        }
        Instant expiresAt = revoked.get(jti);
        if (expiresAt == null) {
            return false;
        }
        if (!expiresAt.isAfter(clock.instant())) {
            revoked.remove(jti);
            return false;
        }
        return true;
    }

    /** Remove entradas cujo token já expirou. Retorna quantas foram removidas. */
    public int purgeExpired() {
        Instant now = clock.instant();
        int before = revoked.size();
        revoked.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
        return before - revoked.size();
    }

    public int size() {
        return revoked.size();
    }
}
