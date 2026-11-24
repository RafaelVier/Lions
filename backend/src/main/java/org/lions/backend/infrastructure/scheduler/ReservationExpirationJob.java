package org.lions.backend.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import org.lions.backend.application.service.waitlist.WaitlistService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * RN33 — a cada 15 minutos: expira reservas não retiradas no prazo (48 h, RN32), oferece o item ao próximo da fila e
 * devolve o beneficiário ao fim da fila (ou o remove após 2 expirações/recusas consecutivas).
 */
@Component
@ConditionalOnSchedulingEnabled
@RequiredArgsConstructor
public class ReservationExpirationJob {

    public static final String DEFAULT_CRON = "0 */15 * * * *";

    private final WaitlistService waitlistService;

    @Scheduled(cron = "${lions.scheduling.cron.reservation-expiration:" + DEFAULT_CRON + "}",
            zone = "${lions.scheduling.zone:}")
    public void run() {
        ScheduledJobRunner.run("ReservationExpirationJob",
                () -> waitlistService.expireReservations() + " reserva(s) expirada(s)");
    }
}
