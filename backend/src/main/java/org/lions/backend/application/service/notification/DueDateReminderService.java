package org.lions.backend.application.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lions.backend.application.service.parameter.ParameterService;
import org.lions.backend.application.service.waitlist.WaitlistService;
import org.lions.backend.application.support.WhatsAppMessageHelper;
import org.lions.backend.domain.entity.Equipment;
import org.lions.backend.domain.entity.Loan;
import org.lions.backend.domain.entity.RenewalPolicy;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.LoanStatus;
import org.lions.backend.domain.enums.NotificationType;
import org.lions.backend.domain.repository.LoanRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.Map;

/**
 * Régua de avisos de devolução (RN46): 7 dias antes, 1 dia antes, no dia do vencimento e, em atraso, no 1º dia e
 * depois a cada {@code notification.overdue-interval-days} (3) dias. Os avisos antes do vencimento dizem se o
 * empréstimo pode ser renovado (RF12/RN23). Um mesmo aviso não se repete no mesmo dia para o mesmo empréstimo
 * (deduplicação por empréstimo + tipo + data). O envio segue os canais autorizados (RN47/RN48).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DueDateReminderService {

    public static final int FIRST_REMINDER_DAYS = 7;
    public static final int SECOND_REMINDER_DAYS = 1;

    private final LoanRepositoryPort loanRepository;
    private final NotificationService notificationService;
    private final WaitlistService waitlistService;
    private final ParameterService parameterService;
    private final WhatsAppMessageHelper whatsApp;
    private final Clock clock;

    /**
     * @param renewableMentioned avisos que informaram que o empréstimo pode ser renovado
     */
    public record Result(int sevenDays, int oneDay, int dueToday, int overdue, int renewableMentioned) {
    }

    @Transactional
    public Result sendReminders() {
        LocalDate today = LocalDate.now(clock);
        RenewalPolicy policy = new RenewalPolicy(parameterService.maxRenewals(), parameterService.renewalWindowDays());
        int overdueInterval = parameterService.overdueIntervalDays();
        Map<EquipmentType, Boolean> waitlistByType = new EnumMap<>(EquipmentType.class);
        int seven = 0;
        int one = 0;
        int dueToday = 0;
        int overdue = 0;
        int renewable = 0;

        for (Loan loan : loanRepository.findOpenDueBetween(today, today.plusDays(FIRST_REMINDER_DAYS))) {
            if (loan.getStatus() != LoanStatus.ATIVO) {
                continue; // PENDENTE_ASSINATURA ainda não foi efetivado (RN21)
            }
            long days = ChronoUnit.DAYS.between(today, loan.getExpectedReturnDate());
            NotificationType type = days == FIRST_REMINDER_DAYS ? NotificationType.LEMBRETE_7_DIAS
                    : days == SECOND_REMINDER_DAYS ? NotificationType.LEMBRETE_1_DIA
                    : days == 0 ? NotificationType.VENCIMENTO_HOJE
                    : null;
            if (type == null) {
                continue;
            }
            EquipmentType equipmentType = loan.getEquipment().getType();
            boolean canRenew = loan.canRenew(today, policy,
                    waitlistByType.computeIfAbsent(equipmentType, waitlistService::hasWaitingFor));
            String name = loan.getBeneficiary().getFullName();
            String message = type == NotificationType.VENCIMENTO_HOJE
                    ? whatsApp.dueTodayMessage(name, describe(loan.getEquipment()), patrimony(loan.getEquipment()),
                    loan.getExpectedReturnDate(), canRenew)
                    : whatsApp.dueDateReminderMessage(name, describe(loan.getEquipment()),
                    patrimony(loan.getEquipment()), loan.getExpectedReturnDate(), canRenew);
            if (notify(loan, type, message, today)) {
                switch (type) {
                    case LEMBRETE_7_DIAS -> seven++;
                    case LEMBRETE_1_DIA -> one++;
                    default -> dueToday++;
                }
                if (canRenew) {
                    renewable++;
                }
            }
        }

        for (Loan loan : loanRepository.findOpenOverdue(today)) {
            if (loan.getStatus() != LoanStatus.ATIVO && loan.getStatus() != LoanStatus.ATRASADO) {
                continue;
            }
            long daysOverdue = loan.daysOverdue(today);
            if (!isOverdueReminderDay(daysOverdue, overdueInterval)) {
                continue;
            }
            String message = whatsApp.overdueMessage(loan.getBeneficiary().getFullName(),
                    describe(loan.getEquipment()), patrimony(loan.getEquipment()), loan.getExpectedReturnDate());
            if (notify(loan, NotificationType.ATRASO, message, today)) {
                overdue++;
            }
        }

        Result result = new Result(seven, one, dueToday, overdue, renewable);
        log.debug("Régua de avisos: {}", result);
        return result;
    }

    /** 1º dia de atraso e depois a cada {@code interval} dias (1, 4, 7, ... com o padrão de 3). */
    static boolean isOverdueReminderDay(long daysOverdue, int interval) {
        return daysOverdue >= 1 && (daysOverdue - 1) % Math.max(interval, 1) == 0;
    }

    private boolean notify(Loan loan, NotificationType type, String message, LocalDate today) {
        return !notificationService.notifyBeneficiary(loan.getBeneficiary().getId(), loan.getId(), type, message,
                today).isEmpty();
    }

    private static String describe(Equipment equipment) {
        return equipment != null && equipment.getType() != null ? equipment.getType().getDescription() : "ortopédico";
    }

    private static String patrimony(Equipment equipment) {
        return equipment != null ? equipment.getPatrimonyNumber() : null;
    }
}
