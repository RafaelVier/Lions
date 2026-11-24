package org.lions.backend.infrastructure.scheduler;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * Executa o corpo de uma rotina agendada registrando duração e resultado. Uma falha é logada e não interrompe o
 * agendador (a próxima execução roda normalmente); a transação do serviço chamado é desfeita por inteiro.
 */
@Slf4j
final class ScheduledJobRunner {

    private ScheduledJobRunner() {
    }

    static <T> T run(String jobName, Supplier<T> body) {
        long start = System.nanoTime();
        try {
            T result = body.get();
            log.info("Rotina {} concluída em {} ms: {}", jobName, (System.nanoTime() - start) / 1_000_000, result);
            return result;
        } catch (RuntimeException e) {
            log.error("Rotina {} falhou após {} ms; nova tentativa na próxima execução.", jobName,
                    (System.nanoTime() - start) / 1_000_000, e);
            return null;
        }
    }
}
