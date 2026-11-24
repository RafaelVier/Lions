package org.lions.backend.application.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.lions.backend.domain.exception.TooManyRequestsException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite de tentativas por chave (ex.: IP e CPF) em janela deslizante, em memória — protege rotas públicas contra
 * enumeração de dados (UC16). Padrão: 5 tentativas a cada 15 minutos por chave.
 */
@Component
public class AttemptLimiter {

    private final Map<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration window;
    private final Clock clock;

    public AttemptLimiter(@Value("${lions.security.public-attempts.max:5}") int maxAttempts,
                          @Value("${lions.security.public-attempts.window-minutes:15}") long windowMinutes,
                          Clock clock) {
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofMinutes(windowMinutes);
        this.clock = clock;
    }

    /** Registra uma tentativa para cada chave; lança 429 se alguma já atingiu o limite na janela. */
    public void acquire(String... keys) {
        Instant now = clock.instant();
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            Deque<Instant> deque = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());
            synchronized (deque) {
                while (!deque.isEmpty() && !deque.peekFirst().isAfter(now.minus(window))) {
                    deque.pollFirst();
                }
                if (deque.size() >= maxAttempts) {
                    throw new TooManyRequestsException("Muitas tentativas. Aguarde alguns minutos e tente novamente.");
                }
            }
        }
        for (String key : keys) {
            if (key != null) {
                Deque<Instant> deque = attempts.get(key);
                synchronized (deque) {
                    deque.addLast(now);
                }
            }
        }
    }
}
