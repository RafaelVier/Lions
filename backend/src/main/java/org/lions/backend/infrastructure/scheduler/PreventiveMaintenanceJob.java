package org.lions.backend.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import org.lions.backend.application.service.maintenance.PreventiveMaintenanceService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * RN39 — diária: avisa o Admin dos equipamentos com revisão preventiva vencida (12 meses sem manutenção ou 10 ciclos
 * de empréstimo), no máximo uma vez por item a cada mês.
 */
@Component
@ConditionalOnSchedulingEnabled
@RequiredArgsConstructor
public class PreventiveMaintenanceJob {

    public static final String DEFAULT_CRON = "0 30 1 * * *";

    private final PreventiveMaintenanceService preventiveMaintenanceService;

    @Scheduled(cron = "${lions.scheduling.cron.preventive-maintenance:" + DEFAULT_CRON + "}",
            zone = "${lions.scheduling.zone:}")
    public void run() {
        ScheduledJobRunner.run("PreventiveMaintenanceJob",
                () -> preventiveMaintenanceService.notifyDue() + " aviso(s) de preventiva criado(s)");
    }
}
