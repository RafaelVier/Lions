package org.lions.backend.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import org.lions.backend.application.service.notification.DailySummaryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** RN49 — diária, 07:00: envia ao Administrador o resumo do dia (notificação interna, uma vez por dia). */
@Component
@ConditionalOnSchedulingEnabled
@RequiredArgsConstructor
public class DailySummaryJob {

    public static final String DEFAULT_CRON = "0 0 7 * * *";

    private final DailySummaryService dailySummaryService;

    @Scheduled(cron = "${lions.scheduling.cron.daily-summary:" + DEFAULT_CRON + "}", zone = "${lions.scheduling.zone:}")
    public void run() {
        ScheduledJobRunner.run("DailySummaryJob",
                () -> dailySummaryService.sendToAdmins() ? "resumo enviado" : "resumo de hoje já enviado");
    }
}
