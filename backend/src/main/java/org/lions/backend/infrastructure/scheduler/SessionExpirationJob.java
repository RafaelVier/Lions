package org.lions.backend.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import org.lions.backend.application.service.auth.SessionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** RN57 — a cada 5 minutos: encerra as sessões que expiraram por inatividade e audita {@code SESSION_EXPIRED}. */
@Component
@ConditionalOnSchedulingEnabled
@RequiredArgsConstructor
public class SessionExpirationJob {

    public static final String DEFAULT_CRON = "0 */5 * * * *";

    private final SessionService sessionService;

    @Scheduled(cron = "${lions.scheduling.cron.session-expiration:" + DEFAULT_CRON + "}",
            zone = "${lions.scheduling.zone:}")
    public void run() {
        ScheduledJobRunner.run("SessionExpirationJob", () -> sessionService.expireInactive() + " sessão(ões) expirada(s)");
    }
}
