package org.lions.backend.infrastructure.scheduler;

import org.junit.jupiter.api.Test;
import org.lions.backend.application.service.auth.SessionService;
import org.lions.backend.application.service.loan.LoanOverdueService;
import org.lions.backend.application.service.maintenance.PreventiveMaintenanceService;
import org.lions.backend.application.service.notification.DailySummaryService;
import org.lions.backend.application.service.notification.DueDateReminderService;
import org.lions.backend.application.service.notification.NotificationService;
import org.lions.backend.application.service.waitlist.WaitlistService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.support.CronExpression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduledJobsTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(LoanOverdueService.class, () -> mock(LoanOverdueService.class))
            .withBean(WaitlistService.class, () -> mock(WaitlistService.class))
            .withBean(NotificationService.class, () -> mock(NotificationService.class))
            .withBean(PreventiveMaintenanceService.class, () -> mock(PreventiveMaintenanceService.class))
            .withBean(DueDateReminderService.class, () -> mock(DueDateReminderService.class))
            .withBean(DailySummaryService.class, () -> mock(DailySummaryService.class))
            .withBean(SessionService.class, () -> mock(SessionService.class))
            .withUserConfiguration(OverdueLoanJob.class, ReservationExpirationJob.class, NotificationRetryJob.class,
                    PreventiveMaintenanceJob.class, DueDateReminderJob.class, DailySummaryJob.class,
                    SessionExpirationJob.class);

    @Test
    void deveRegistrarJobsQuandoAgendamentoHabilitado() {
        contextRunner.withPropertyValues("lions.scheduling.enabled=true").run(context -> {
            assertThat(context).hasSingleBean(OverdueLoanJob.class);
            assertThat(context).hasSingleBean(ReservationExpirationJob.class);
            assertThat(context).hasSingleBean(NotificationRetryJob.class);
            assertThat(context).hasSingleBean(PreventiveMaintenanceJob.class);
            assertThat(context).hasSingleBean(DueDateReminderJob.class);
            assertThat(context).hasSingleBean(DailySummaryJob.class);
            assertThat(context).hasSingleBean(SessionExpirationJob.class);
        });
    }

    @Test
    void deveRegistrarJobsQuandoPropriedadeAusente() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(OverdueLoanJob.class));
    }

    @Test
    void naoDeveRegistrarJobsQuandoAgendamentoDesabilitado() {
        contextRunner.withPropertyValues("lions.scheduling.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(OverdueLoanJob.class);
            assertThat(context).doesNotHaveBean(ReservationExpirationJob.class);
            assertThat(context).doesNotHaveBean(NotificationRetryJob.class);
            assertThat(context).doesNotHaveBean(PreventiveMaintenanceJob.class);
            assertThat(context).doesNotHaveBean(DueDateReminderJob.class);
            assertThat(context).doesNotHaveBean(DailySummaryJob.class);
            assertThat(context).doesNotHaveBean(SessionExpirationJob.class);
        });
    }

    @Test
    void deveTerCronValidoQuandoUsaPadrao() {
        for (String cron : new String[]{OverdueLoanJob.DEFAULT_CRON, ReservationExpirationJob.DEFAULT_CRON,
                NotificationRetryJob.DEFAULT_CRON, PreventiveMaintenanceJob.DEFAULT_CRON, DueDateReminderJob.DEFAULT_CRON,
                DailySummaryJob.DEFAULT_CRON, SessionExpirationJob.DEFAULT_CRON}) {
            assertThat(CronExpression.isValidExpression(cron)).as(cron).isTrue();
        }
    }

    @Test
    void deveDelegarParaOsServicosQuandoExecutado() {
        LoanOverdueService overdue = mock(LoanOverdueService.class);
        when(overdue.processOverdueLoans()).thenReturn(new LoanOverdueService.Result(2, 1));
        WaitlistService waitlist = mock(WaitlistService.class);
        NotificationService notifications = mock(NotificationService.class);
        PreventiveMaintenanceService preventive = mock(PreventiveMaintenanceService.class);
        DueDateReminderService reminders = mock(DueDateReminderService.class);
        DailySummaryService summary = mock(DailySummaryService.class);

        new OverdueLoanJob(overdue).run();
        new DueDateReminderJob(reminders).run();
        new DailySummaryJob(summary).run();
        new ReservationExpirationJob(waitlist).run();
        new NotificationRetryJob(notifications).run();
        new PreventiveMaintenanceJob(preventive).run();

        verify(overdue).processOverdueLoans();
        verify(waitlist).expireReservations();
        verify(notifications).retryFailed();
        verify(preventive).notifyDue();
        verify(reminders).sendReminders();
        verify(summary).sendToAdmins();
    }

    @Test
    void naoDevePropagarExcecaoQuandoServicoFalha() {
        LoanOverdueService overdue = mock(LoanOverdueService.class);
        when(overdue.processOverdueLoans()).thenThrow(new IllegalStateException("banco fora do ar"));
        WaitlistService waitlist = mock(WaitlistService.class);
        when(waitlist.expireReservations()).thenThrow(new IllegalStateException("falha"));

        assertThatCode(() -> new OverdueLoanJob(overdue).run()).doesNotThrowAnyException();
        assertThatCode(() -> new ReservationExpirationJob(waitlist).run()).doesNotThrowAnyException();
    }
}
