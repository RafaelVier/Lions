package org.lions.backend.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAndRevocationTest {

    private static final Instant T0 = Instant.parse("2025-03-10T12:00:00Z");

    /** Relógio ajustável para simular passagem do tempo. */
    static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
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
            return instant;
        }
    }

    private static UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken("marcio", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    // ---------- JwtTokenProvider ----------

    @Test
    void deveGerarTokenComJtiEExpiracaoDe30MinutosQuandoAutenticado() {
        MutableClock clock = new MutableClock(T0);
        JwtTokenProvider provider = new JwtTokenProvider(JwtTokenProvider.DEFAULT_DEV_SECRET, 1_800_000L, clock);

        String token = provider.generateToken(auth());
        String other = provider.generateToken(auth());

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUsernameFromToken(token)).isEqualTo("marcio");
        assertThat(provider.getJti(token)).isNotBlank().isNotEqualTo(provider.getJti(other));
        assertThat(provider.getExpiration(token)).isEqualTo(T0.plus(Duration.ofMinutes(30)));
    }

    @Test
    void deveInvalidarTokenQuandoExpiradoOuAdulterado() {
        MutableClock clock = new MutableClock(T0);
        JwtTokenProvider provider = new JwtTokenProvider(JwtTokenProvider.DEFAULT_DEV_SECRET, 1_800_000L, clock);
        String token = provider.generateToken(auth());

        String[] parts = token.split("\\.");
        String forged = parts[0] + "." + parts[1] + "." + new StringBuilder(parts[2]).reverse();
        JwtTokenProvider otherKey = new JwtTokenProvider(
                "6E5A7234753778214125442A472D4B6150645367566B59703373367639792F42", 1_800_000L, clock);
        assertThat(provider.validateToken(forged)).isFalse();
        assertThat(provider.validateToken(otherKey.generateToken(auth()))).isFalse();
        assertThat(provider.validateToken("nao-e-um-jwt")).isFalse();

        clock.advance(Duration.ofMinutes(31));
        assertThat(provider.validateToken(token)).isFalse();
    }

    // ---------- TokenRevocationService ----------

    @Test
    void deveIndicarRevogadoQuandoJtiRevogadoAteExpirar() {
        MutableClock clock = new MutableClock(T0);
        TokenRevocationService service = new TokenRevocationService(clock);

        service.revoke("jti-1", T0.plus(Duration.ofMinutes(30)));

        assertThat(service.isRevoked("jti-1")).isTrue();
        assertThat(service.isRevoked("jti-2")).isFalse();
        assertThat(service.isRevoked(null)).isFalse();

        clock.advance(Duration.ofMinutes(31));
        assertThat(service.isRevoked("jti-1")).isFalse();
        assertThat(service.size()).isZero();
    }

    @Test
    void deveLimparEntradasExpiradasQuandoPurgeExecutado() {
        MutableClock clock = new MutableClock(T0);
        TokenRevocationService service = new TokenRevocationService(clock);
        service.revoke("a", T0.plus(Duration.ofMinutes(5)));
        service.revoke("b", T0.plus(Duration.ofMinutes(60)));
        service.revoke(" ", T0.plus(Duration.ofMinutes(60)));

        clock.advance(Duration.ofMinutes(10));

        assertThat(service.purgeExpired()).isEqualTo(1);
        assertThat(service.size()).isEqualTo(1);
        assertThat(service.isRevoked("b")).isTrue();
    }
}
