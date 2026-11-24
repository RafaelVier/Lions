package org.lions.backend.application.service.notification;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lions.backend.application.dto.request.DigitalSignatureRequest;
import org.lions.backend.application.dto.request.EquipmentCreateRequest;
import org.lions.backend.application.dto.request.LoanCreateRequest;
import org.lions.backend.application.dto.request.MaintenanceCreateRequest;
import org.lions.backend.application.dto.response.DailySummaryResponse;
import org.lions.backend.application.service.beneficiary.BeneficiaryService;
import org.lions.backend.application.service.equipment.EquipmentService;
import org.lions.backend.application.service.loan.LoanService;
import org.lions.backend.application.service.maintenance.MaintenanceService;
import org.lions.backend.application.service.waitlist.WaitlistService;
import org.lions.backend.domain.entity.Beneficiary;
import org.lions.backend.domain.entity.Notification;
import org.lions.backend.domain.entity.WaitlistEntry;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.NotificationType;
import org.lions.backend.domain.enums.RecipientType;
import org.lions.backend.domain.repository.BeneficiaryRepositoryPort;
import org.lions.backend.domain.repository.NotificationRepositoryPort;
import org.lions.backend.domain.repository.WaitlistEntryRepositoryPort;
import org.lions.backend.support.TestFixtures;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** E.4 (RN46, RN49) e G.11 (RF12) pelos serviços reais. Prazo padrão de 30 dias (RN18). */
@SpringBootTest
@Transactional
class RemindersAndDailySummaryIT {

    @Autowired
    private BeneficiaryService beneficiaryService;
    @Autowired
    private EquipmentService equipmentService;
    @Autowired
    private LoanService loanService;
    @Autowired
    private MaintenanceService maintenanceService;
    @Autowired
    private WaitlistService waitlistService;
    @Autowired
    private DueDateReminderService reminderService;
    @Autowired
    private DailySummaryService dailySummaryService;
    @Autowired
    private BeneficiaryRepositoryPort beneficiaryRepository;
    @Autowired
    private WaitlistEntryRepositoryPort waitlistRepository;
    @Autowired
    private NotificationRepositoryPort notificationRepository;

    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void authenticate() {
        TestFixtures.authenticateAs("admin", "ADMIN");
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private Long equipment(EquipmentType type) {
        return equipmentService.create(EquipmentCreateRequest.builder()
                .type(type).condition(EquipmentCondition.BOM).build()).getId();
    }

    /** Empréstimo ativo (assinado) com vencimento em {@code today + daysToDue} (prazo de 30 dias). */
    private Loaned loanDueIn(int daysToDue, EquipmentType type) {
        Long beneficiaryId = beneficiaryService.create(TestFixtures.beneficiaryRequest("Benef " + daysToDue)).id();
        LocalDate loanDate = today.plusDays(daysToDue).minusDays(30);
        Long loanId = loanService.registerLoan(LoanCreateRequest.builder()
                .beneficiaryId(beneficiaryId).equipmentId(equipment(type))
                .loanDate(loanDate.isBefore(today) ? loanDate : null)
                .retroactiveJustification("Empréstimo registrado no papel")
                .build()).getId();
        loanService.registerDigitalSignature(loanId, new DigitalSignatureRequest(null));
        return new Loaned(beneficiaryId, loanId);
    }

    private record Loaned(Long beneficiaryId, Long loanId) {
    }

    private List<Notification> notifications(Long beneficiaryId, NotificationType type) {
        return notificationRepository.findByBeneficiaryId(beneficiaryId).stream()
                .filter(n -> n.getType() == type).toList();
    }

    @Test
    void deveEnviarReguaDeAvisosUmaVezPorDiaQuandoRotinaExecuta() {
        Loaned seven = loanDueIn(7, EquipmentType.ANDADOR);
        Loaned one = loanDueIn(1, EquipmentType.ANDADOR);
        Loaned dueToday = loanDueIn(0, EquipmentType.ANDADOR);
        Loaned overdue1 = loanDueIn(-1, EquipmentType.ANDADOR);
        Loaned overdue2 = loanDueIn(-2, EquipmentType.ANDADOR);
        Loaned overdue4 = loanDueIn(-4, EquipmentType.ANDADOR);
        Loaned five = loanDueIn(5, EquipmentType.ANDADOR);

        reminderService.sendReminders();

        assertThat(notifications(seven.beneficiaryId(), NotificationType.LEMBRETE_7_DIAS)).isNotEmpty();
        assertThat(notifications(one.beneficiaryId(), NotificationType.LEMBRETE_1_DIA)).isNotEmpty();
        assertThat(notifications(dueToday.beneficiaryId(), NotificationType.VENCIMENTO_HOJE)).isNotEmpty();
        assertThat(notifications(overdue1.beneficiaryId(), NotificationType.ATRASO)).isNotEmpty();
        assertThat(notifications(overdue4.beneficiaryId(), NotificationType.ATRASO)).isNotEmpty();
        assertThat(notificationRepository.findByBeneficiaryId(overdue2.beneficiaryId())).isEmpty();
        assertThat(notificationRepository.findByBeneficiaryId(five.beneficiaryId())).isEmpty();

        int before = notificationRepository.findByBeneficiaryId(seven.beneficiaryId()).size();
        DueDateReminderService.Result again = reminderService.sendReminders();
        assertThat(again).isEqualTo(new DueDateReminderService.Result(0, 0, 0, 0, 0));
        assertThat(notificationRepository.findByBeneficiaryId(seven.beneficiaryId())).hasSize(before);
    }

    @Test
    void deveInformarRenovacaoSomenteQuandoElegivel() {
        Loaned renewable = loanDueIn(7, EquipmentType.ANDADOR);
        Loaned withQueue = loanDueIn(7, EquipmentType.MULETAS);
        // fila aguardando para MULETAS impede a renovação (RN23)
        Beneficiary waiting = beneficiaryRepository.findById(
                beneficiaryService.create(TestFixtures.beneficiaryRequest("Na fila")).id()).orElseThrow();
        waitlistRepository.save(WaitlistEntry.builder().beneficiary(waiting).equipmentType(EquipmentType.MULETAS)
                .queuedAt(LocalDateTime.now()).originalRequestedAt(LocalDateTime.now()).createdBy("teste").build());

        reminderService.sendReminders();

        assertThat(notifications(renewable.beneficiaryId(), NotificationType.LEMBRETE_7_DIAS))
                .allSatisfy(n -> assertThat(n.getMessage()).contains("pode ser renovado"));
        assertThat(notifications(withQueue.beneficiaryId(), NotificationType.LEMBRETE_7_DIAS))
                .isNotEmpty()
                .allSatisfy(n -> assertThat(n.getMessage()).doesNotContain("renov"));
    }

    @Test
    void deveMontarEEnviarResumoDiarioQuandoHaPendencias() {
        Loaned overdue = loanDueIn(-3, EquipmentType.ANDADOR);
        Long inMaintenance = equipment(EquipmentType.CADEIRA_DE_RODAS);
        maintenanceService.open(new MaintenanceCreateRequest(inMaintenance, null, "Freio solto", null, "Oficina X"));
        Beneficiary reserved = beneficiaryRepository.findById(
                beneficiaryService.create(TestFixtures.beneficiaryRequest("Reserva vencida")).id()).orElseThrow();
        WaitlistEntry entry = WaitlistEntry.builder().beneficiary(reserved).equipmentType(EquipmentType.CADEIRA_DE_BANHO)
                .queuedAt(LocalDateTime.now().minusDays(5)).originalRequestedAt(LocalDateTime.now().minusDays(5))
                .createdBy("teste").build();
        entry.reserve(equipment(EquipmentType.CADEIRA_DE_BANHO), LocalDateTime.now().minusHours(49), 48);
        Long entryId = waitlistRepository.save(entry).getId();
        waitlistService.expireReservations();

        DailySummaryResponse summary = dailySummaryService.build();

        assertThat(summary.date()).isEqualTo(today);
        assertThat(summary.overdueLoans()).anySatisfy(l -> {
            assertThat(l.loanId()).isEqualTo(overdue.loanId());
            assertThat(l.daysOverdue()).isEqualTo(3);
        });
        assertThat(summary.openMaintenances()).anySatisfy(m -> assertThat(m.equipmentId()).isEqualTo(inMaintenance));
        assertThat(summary.expiredReservations()).anySatisfy(r -> {
            assertThat(r.waitlistEntryId()).isEqualTo(entryId);
            assertThat(r.beneficiaryName()).isEqualTo("Reserva vencida");
        });

        assertThat(dailySummaryService.sendToAdmins()).isTrue();
        assertThat(dailySummaryService.sendToAdmins()).isFalse();
        assertThat(notificationRepository.findAll(null, NotificationType.RESUMO_DIARIO))
                .filteredOn(n -> n.getRecipientType() == RecipientType.ADMINISTRADOR)
                .anySatisfy(n -> assertThat(n.getMessage())
                        .contains("Resumo diário de", "empréstimo(s) em atraso", "manutenção(ões) em aberto",
                                "reserva(s) expirada(s)"));
    }
}
