package org.lions.backend.application.service.loan;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.lions.backend.application.dto.request.DigitalSignatureRequest;
import org.lions.backend.application.dto.request.EquipmentCreateRequest;
import org.lions.backend.application.dto.request.LoanCreateRequest;
import org.lions.backend.application.dto.request.LoanReturnRequest;
import org.lions.backend.application.dto.request.MaintenanceCloseRequest;
import org.lions.backend.application.dto.response.LoanResponse;
import org.lions.backend.application.dto.response.MaintenanceResponse;
import org.lions.backend.application.service.beneficiary.BeneficiaryService;
import org.lions.backend.application.service.equipment.EquipmentService;
import org.lions.backend.application.service.maintenance.MaintenanceService;
import org.lions.backend.domain.entity.Occurrence;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.LoanStatus;
import org.lions.backend.domain.enums.MaintenanceStatus;
import org.lions.backend.domain.enums.MaintenanceVerdict;
import org.lions.backend.domain.enums.OccurrenceStatus;
import org.lions.backend.domain.enums.OccurrenceType;
import org.lions.backend.domain.enums.ReturnCondition;
import org.lions.backend.domain.exception.ForbiddenOperationException;
import org.lions.backend.domain.repository.OccurrenceRepositoryPort;
import org.lions.backend.infrastructure.scheduler.OverdueLoanJob;
import org.lions.backend.support.TestFixtures;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** G.8 (RN40), G.9 (UC23) e a rotina de atraso (RN27) pelos serviços reais. */
@SpringBootTest
@Transactional
class ReturnDamageAndMaintenanceIT {

    @Autowired
    private BeneficiaryService beneficiaryService;
    @Autowired
    private EquipmentService equipmentService;
    @Autowired
    private LoanService loanService;
    @Autowired
    private MaintenanceService maintenanceService;
    @Autowired
    private BeneficiaryRestrictionService restrictionService;
    @Autowired
    private LoanOverdueService loanOverdueService;
    @Autowired
    private OccurrenceRepositoryPort occurrenceRepository;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private static void as(String username, String role) {
        TestFixtures.authenticateAs(username, role);
    }

    private Long beneficiary() {
        return beneficiaryService.create(TestFixtures.beneficiaryRequest("Maria Teste")).id();
    }

    private Long equipment() {
        return equipmentService.create(EquipmentCreateRequest.builder()
                .type(EquipmentType.ANDADOR).condition(EquipmentCondition.BOM).build()).getId();
    }

    private LoanResponse activeLoan(Long beneficiaryId, Long equipmentId, LocalDate loanDate) {
        LoanResponse loan = loanService.registerLoan(LoanCreateRequest.builder()
                .beneficiaryId(beneficiaryId).equipmentId(equipmentId).loanDate(loanDate)
                .retroactiveJustification(loanDate != null ? "Registro de empréstimo feito no papel" : null)
                .build());
        return loanService.registerDigitalSignature(loan.getId(), new DigitalSignatureRequest(null));
    }

    @Test
    void deveGerarOcorrenciaEBloquearQuandoDevolucaoPrecisaManutencao() {
        as("operador", "OPERATOR");
        Long beneficiaryId = beneficiary();
        Long equipmentId = equipment();
        LoanResponse loan = activeLoan(beneficiaryId, equipmentId, null);

        loanService.registerReturn(loan.getId(),
                new LoanReturnRequest(ReturnCondition.PRECISA_MANUTENCAO, null, null, null));

        List<Occurrence> occurrences = occurrenceRepository.findByBeneficiaryId(beneficiaryId);
        assertThat(occurrences).singleElement().satisfies(o -> {
            assertThat(o.getType()).isEqualTo(OccurrenceType.DANO);
            assertThat(o.getStatus()).isEqualTo(OccurrenceStatus.ABERTA);
            assertThat(o.getLoanId()).isEqualTo(loan.getId());
            assertThat(o.getDescription()).contains("sem descrição");
        });
        assertThat(restrictionService.evaluate(beneficiaryId).blocked()).isTrue();
        assertThat(equipmentService.findById(equipmentId).getStatus()).isEqualTo(EquipmentStatus.EM_MANUTENCAO);
    }

    @Test
    void naoDeveGerarOcorrenciaQuandoDevolucaoEmBomEstado() {
        as("operador", "OPERATOR");
        Long beneficiaryId = beneficiary();
        LoanResponse loan = activeLoan(beneficiaryId, equipment(), null);

        loanService.registerReturn(loan.getId(), new LoanReturnRequest(ReturnCondition.BOM_ESTADO, null, null, null));

        assertThat(occurrenceRepository.findByBeneficiaryId(beneficiaryId)).isEmpty();
        assertThat(restrictionService.evaluate(beneficiaryId).blocked()).isFalse();
    }

    @Test
    void deveEncerrarManutencaoSomenteQuandoAdmin() {
        as("operador", "OPERATOR");
        Long equipmentId = equipment();
        LoanResponse loan = activeLoan(beneficiary(), equipmentId, null);
        loanService.registerReturn(loan.getId(),
                new LoanReturnRequest(ReturnCondition.DANIFICADO, "Roda solta", null, null));
        MaintenanceResponse open = maintenanceService.findAll(MaintenanceStatus.EM_ANDAMENTO, equipmentId, null)
                .get(0);

        assertThatThrownBy(() -> maintenanceService.close(open.id(),
                new MaintenanceCloseRequest(MaintenanceVerdict.APTO, "Roda trocada")))
                .isInstanceOf(ForbiddenOperationException.class);

        as("admin", "ADMIN");
        MaintenanceResponse closed = maintenanceService.close(open.id(),
                new MaintenanceCloseRequest(MaintenanceVerdict.APTO, "Roda trocada"));
        assertThat(closed.status()).isEqualTo(MaintenanceStatus.CONCLUIDA_APTO);
        assertThat(equipmentService.findById(equipmentId).getStatus()).isEqualTo(EquipmentStatus.DISPONIVEL);
    }

    @Test
    void deveMarcarAtrasoQuandoRotinaDeAtrasoExecuta() {
        as("admin", "ADMIN");
        Long beneficiaryId = beneficiary();
        LoanResponse loan = activeLoan(beneficiaryId, equipment(), LocalDate.now().minusDays(40));
        SecurityContextHolder.clearContext(); // a rotina roda sem usuário autenticado (SISTEMA)

        new OverdueLoanJob(loanOverdueService).run();

        assertThat(loanService.findById(loan.getId()).getStatus()).isEqualTo(LoanStatus.ATRASADO);
        assertThat(restrictionService.evaluate(beneficiaryId).blocked()).isTrue();
    }
}
