package org.lions.backend.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import org.lions.backend.application.service.notification.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * RN48 — a cada 15 minutos: tenta de novo as notificações pendentes/com falha abaixo do limite de tentativas (3);
 * esgotado o limite, a notificação vira tarefa de contato manual para o Admin.
 */
@Component
@ConditionalOnSchedulingEnabled
@RequiredArgsConstructor
public class NotificationRetryJob {

    public static final String DEFAULT_CRON = "0 5/15 * * * *";

    private final NotificationService notificationService;

    @Scheduled(cron = "${lions.scheduling.cron.notification-retry:" + DEFAULT_CRON + "}",
            zone = "${lions.scheduling.zone:}")
    public void run() {
        ScheduledJobRunner.run("NotificationRetryJob",
                () -> notificationService.retryFailed() + " notificação(ões) processada(s)");
    }
}
