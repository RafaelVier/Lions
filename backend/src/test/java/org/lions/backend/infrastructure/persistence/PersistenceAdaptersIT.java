package org.lions.backend.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.lions.backend.domain.entity.Beneficiary;
import org.lions.backend.domain.entity.Equipment;
import org.lions.backend.domain.entity.Loan;
import org.lions.backend.domain.entity.LoanOrder;
import org.lions.backend.domain.entity.Maintenance;
import org.lions.backend.domain.entity.WaitlistEntry;
import org.lions.backend.domain.enums.CostType;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.LoanStatus;
import org.lions.backend.domain.enums.MaintenanceType;
import org.lions.backend.domain.enums.NotificationChannel;
import org.lions.backend.domain.enums.PriorityReason;
import org.lions.backend.domain.repository.BeneficiaryRepositoryPort;
import org.lions.backend.domain.repository.EquipmentRepositoryPort;
import org.lions.backend.domain.repository.LoanOrderRepositoryPort;
import org.lions.backend.domain.repository.LoanRepositoryPort;
import org.lions.backend.domain.repository.MaintenanceRepositoryPort;
import org.lions.backend.domain.repository.WaitlistEntryRepositoryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class PersistenceAdaptersIT {

    private static final LocalDate TODAY = LocalDate.of(2025, 3, 10);
    private static final LocalDateTime NOW = TODAY.atTime(10, 0);

    @Autowired
    private EquipmentRepositoryPort equipmentRepository;
    @Autowired
    private BeneficiaryRepositoryPort beneficiaryRepository;
    @Autowired
    private LoanOrderRepositoryPort orderRepository;
    @Autowired
    private LoanRepositoryPort loanRepository;
    @Autowired
    private WaitlistEntryRepositoryPort waitlistRepository;
    @Autowired
    private MaintenanceRepositoryPort maintenanceRepository;

    static Equipment newEquipment(EquipmentType type) {
        return Equipment.builder().type(type).condition(EquipmentCondition.BOM).size("Adulto").build();
    }

    private Beneficiary beneficiary(String name) {
        Beneficiary b = Beneficiary.builder()
                .fullName(name)
                .documentNumber(UUID.randomUUID().toString().substring(0, 11))
                .birthDate(LocalDate.of(1960, 1, 1))
                .phone("45999990000")
                .city("Medianeira")
                .state("PR")
                .build();
        b.grantConsent(Set.of(NotificationChannel.WHATSAPP, NotificationChannel.SMS), name, NOW);
        return beneficiaryRepository.save(b);
    }

    @Test
    void deveGerarPatrimonioPeloIdQuandoSalvaNovoEquipamento() {
        Equipment saved = equipmentRepository.saveNew(newEquipment(EquipmentType.ANDADOR));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPatrimonyNumber()).isEqualTo(String.format("PT-%04d", saved.getId()));
        assertThat(saved.getStatus()).isEqualTo(EquipmentStatus.DISPONIVEL);
        assertThat(saved.getVersion()).isNotNull();
        assertThat(saved.getSize()).isEqualTo("Adulto");
        assertThat(equipmentRepository.findByPatrimonyNumber(saved.getPatrimonyNumber())).isPresent();
    }

    @Test
    void deveDelegarParaSaveNewQuandoSaveSemId() {
        Equipment saved = equipmentRepository.save(newEquipment(EquipmentType.MULETAS));
        assertThat(saved.getPatrimonyNumber()).isEqualTo(Equipment.formatPatrimony(saved.getId()));
    }

    @Test
    void deveBloquearRegistroQuandoFindByIdForUpdateEmTransacao() {
        Equipment saved = equipmentRepository.saveNew(newEquipment(EquipmentType.ANDADOR));
        Equipment locked = equipmentRepository.findByIdForUpdate(saved.getId()).orElseThrow();
        assertThat(locked.getPatrimonyNumber()).isEqualTo(saved.getPatrimonyNumber());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deveLancarConflitoOtimistaQuandoVersaoDesatualizada() {
        Equipment saved = equipmentRepository.saveNew(newEquipment(EquipmentType.ANDADOR));
        Equipment copyA = equipmentRepository.findById(saved.getId()).orElseThrow();
        Equipment copyB = equipmentRepository.findById(saved.getId()).orElseThrow();

        copyA.setNotes("Alterado por A");
        Equipment updated = equipmentRepository.save(copyA);
        assertThat(updated.getVersion()).isGreaterThan(copyB.getVersion());

        copyB.setNotes("Alterado por B");
        assertThatThrownBy(() -> equipmentRepository.save(copyB))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(equipmentRepository.findById(saved.getId()).orElseThrow().getNotes()).isEqualTo("Alterado por A");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deveExigirTransacaoQuandoFindByIdForUpdate() {
        assertThatThrownBy(() -> equipmentRepository.findByIdForUpdate(1L))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void deveMapearBeneficiarioComCanaisQuandoSalvo() {
        Beneficiary saved = beneficiary("Maria Teste");
        Beneficiary loaded = beneficiaryRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getNotificationChannels())
                .containsExactlyInAnyOrder(NotificationChannel.WHATSAPP, NotificationChannel.SMS);
        assertThat(loaded.isConsentGranted()).isTrue();
        assertThat(loaded.getCreatedAt()).isNotNull();
    }

    @Test
    void deveMapearGrafoQuandoEmprestimoSalvo() {
        Beneficiary b = beneficiary("José Grafo");
        Equipment e = equipmentRepository.saveNew(newEquipment(EquipmentType.CADEIRA_DE_BANHO));
        LoanOrder order = orderRepository.save(LoanOrder.builder()
                .beneficiary(b).equipmentType(EquipmentType.CADEIRA_DE_BANHO).needReason("Pós-cirurgia").build());

        Loan saved = loanRepository.save(Loan.builder()
                .beneficiary(b)
                .equipment(e)
                .order(order)
                .status(LoanStatus.ATIVO)
                .loanDate(TODAY.minusDays(40))
                .initialExpectedReturnDate(TODAY.minusDays(10))
                .expectedReturnDate(TODAY.minusDays(10))
                .initialCondition(EquipmentCondition.BOM)
                .build());

        Loan loaded = loanRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getBeneficiary().getFullName()).isEqualTo("José Grafo");
        assertThat(loaded.getEquipment().getPatrimonyNumber()).isEqualTo(e.getPatrimonyNumber());
        assertThat(loaded.getOrder().getId()).isEqualTo(order.getId());
        assertThat(loanRepository.findOpenByBeneficiaryId(b.getId())).extracting(Loan::getId)
                .containsExactly(saved.getId());
        assertThat(loanRepository.findOpenOverdue(TODAY)).extracting(Loan::getId).contains(saved.getId());
    }

    @Test
    void deveOrdenarFilaPorPrioridadeEDataQuandoListaAguardando() {
        Beneficiary first = beneficiary("Primeiro");
        Beneficiary second = beneficiary("Segundo");
        Beneficiary priority = beneficiary("Prioritário");
        Beneficiary reserved = beneficiary("Reservado");
        EquipmentType type = EquipmentType.OUTROS;

        WaitlistEntry e1 = waitlistRepository.save(entry(first, type, NOW.minusDays(10)));
        WaitlistEntry e2 = waitlistRepository.save(entry(second, type, NOW.minusDays(5)));
        WaitlistEntry p = entry(priority, type, NOW.minusDays(1));
        p.setPriority(PriorityReason.URGENCIA_SAUDE, "Alta hospitalar", "admin");
        WaitlistEntry e3 = waitlistRepository.save(p);
        WaitlistEntry r = entry(reserved, type, NOW.minusDays(20));
        r.reserve(999L, NOW, 48);
        waitlistRepository.save(r);

        List<WaitlistEntry> queue = waitlistRepository.findActiveByType(type);

        assertThat(queue).extracting(WaitlistEntry::getId)
                .containsSubsequence(e3.getId(), e1.getId(), e2.getId())
                .doesNotContain(r.getId());
        assertThat(queue.get(0).getBeneficiary().getFullName()).isEqualTo("Prioritário");
        assertThat(waitlistRepository.existsActiveByBeneficiaryIdAndType(reserved.getId(), type)).isTrue();
        assertThat(waitlistRepository.findExpiredReservations(NOW.plusHours(49)))
                .extracting(WaitlistEntry::getBeneficiary)
                .extracting(Beneficiary::getId)
                .contains(reserved.getId());
    }

    private static WaitlistEntry entry(Beneficiary b, EquipmentType type, LocalDateTime queuedAt) {
        return WaitlistEntry.builder()
                .beneficiary(b)
                .equipmentType(type)
                .queuedAt(queuedAt)
                .originalRequestedAt(queuedAt)
                .build();
    }

    @Test
    void devePersistirCustosEmCascataQuandoManutencaoSalva() {
        Equipment e = equipmentRepository.saveNew(newEquipment(EquipmentType.CADEIRA_DE_RODAS));
        Maintenance m = Maintenance.builder()
                .equipment(e)
                .type(MaintenanceType.CORRETIVA)
                .defectDescription("Pneu furado")
                .entryDate(TODAY)
                .technicalResponsible("Oficina")
                .build();
        m.validateOpening();
        Maintenance saved = maintenanceRepository.save(m);

        saved.addCost(CostType.PECA, "Câmara de ar", "Câmara", new BigDecimal("35.90"), "op", NOW);
        saved.addCost(CostType.SERVICO, "Troca", null, new BigDecimal("20.00"), "op", NOW);
        maintenanceRepository.save(saved);

        Maintenance loaded = maintenanceRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getCosts()).hasSize(2).allSatisfy(c -> {
            assertThat(c.getId()).isNotNull();
            assertThat(c.getMaintenanceId()).isEqualTo(saved.getId());
        });
        assertThat(loaded.totalCost()).isEqualByComparingTo("55.90");
        assertThat(loaded.getEquipment().getPatrimonyNumber()).isEqualTo(e.getPatrimonyNumber());
    }
}
