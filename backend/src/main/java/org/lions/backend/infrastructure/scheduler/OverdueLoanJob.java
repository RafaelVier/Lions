package org.lions.backend.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import org.lions.backend.application.service.loan.LoanOverdueService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * RN27 — diária: marca empréstimos vencidos como ATRASADO, registra inadimplência com 30 dias de atraso, avisa o
 * Admin e remove o beneficiário da fila (RN34). Ações auditadas como automáticas (RN52).
 */
@Component
@ConditionalOnSchedulingEnabled
@RequiredArgsConstructor
public class OverdueLoanJob {

    public static final String DEFAULT_CRON = "0 0 1 * * *";

    private final LoanOverdueService loanOverdueService;

    @Scheduled(cron = "${lions.scheduling.cron.overdue-loans:" + DEFAULT_CRON + "}", zone = "${lions.scheduling.zone:}")
    public void run() {
        ScheduledJobRunner.run("OverdueLoanJob", loanOverdueService::processOverdueLoans);
    }
}
