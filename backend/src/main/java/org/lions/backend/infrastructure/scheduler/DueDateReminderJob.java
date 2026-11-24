package org.lions.backend.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import org.lions.backend.application.service.notification.DueDateReminderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * RN46/RF12 — diária, de manhã: avisos de 7 dias, 1 dia e no dia do vencimento (informando se pode renovar) e avisos
 * de atraso (1º dia e a cada 3 dias). Roda depois da rotina de atraso (01:00).
 */
@Component
@ConditionalOnSchedulingEnabled
@RequiredArgsConstructor
public class DueDateReminderJob {

    public static final String DEFAULT_CRON = "0 0 8 * * *";

    private final DueDateReminderService dueDateReminderService;

    @Scheduled(cron = "${lions.scheduling.cron.due-date-reminders:" + DEFAULT_CRON + "}",
            zone = "${lions.scheduling.zone:}")
    public void run() {
        ScheduledJobRunner.run("DueDateReminderJob", dueDateReminderService::sendReminders);
    }
}
