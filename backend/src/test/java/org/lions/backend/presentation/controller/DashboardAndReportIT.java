package org.lions.backend.presentation.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.lions.backend.application.dto.request.DigitalSignatureRequest;
import org.lions.backend.application.dto.request.EquipmentCreateRequest;
import org.lions.backend.application.dto.request.LoanCreateRequest;
import org.lions.backend.application.dto.request.MaintenanceCostRequest;
import org.lions.backend.application.dto.request.MaintenanceCreateRequest;
import org.lions.backend.application.dto.request.WriteOffRequest;
import org.lions.backend.application.dto.response.BeneficiaryResponse;
import org.lions.backend.application.dto.response.LoanResponse;
import org.lions.backend.application.service.beneficiary.BeneficiaryService;
import org.lions.backend.application.service.equipment.EquipmentService;
import org.lions.backend.application.service.loan.LoanService;
import org.lions.backend.application.service.maintenance.MaintenanceService;
import org.lions.backend.domain.entity.AuditLog;
import org.lions.backend.domain.enums.AuditAction;
import org.lions.backend.domain.enums.CostType;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.WriteOffReason;
import org.lions.backend.domain.repository.AuditLogRepositoryPort;
import org.lions.backend.support.TestFixtures;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Frente F — dashboard (RF15, RN54–RN56) e relatórios (RF11, RN43–RN45, RN55). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardAndReportIT {

    private static final RequestPostProcessor ADMIN = user("admin.teste").roles("ADMIN");
    private static final RequestPostProcessor VIEWER = user("consulta.teste").roles("VIEWER");
    private static final EquipmentType TYPE = EquipmentType.CADEIRA_DE_BANHO;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private BeneficiaryService beneficiaryService;
    @Autowired
    private EquipmentService equipmentService;
    @Autowired
    private LoanService loanService;
    @Autowired
    private MaintenanceService maintenanceService;
    @Autowired
    private AuditLogRepositoryPort auditLogRepository;

    private final LocalDate today = LocalDate.now();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private Long equipment() {
        return equipmentService.create(EquipmentCreateRequest.builder().type(TYPE).condition(EquipmentCondition.BOM)
                .build()).getId();
    }

    private record Data(BeneficiaryResponse beneficiary, LoanResponse overdueLoan) {
    }

    /** 1 emprestado em atraso, 1 em manutenção com custo, 1 baixado, 1 disponível (todos cadeira de banho). */
    private Data seed() {
        TestFixtures.authenticateAs("admin", "ADMIN");
        BeneficiaryResponse b = beneficiaryService.create(TestFixtures.beneficiaryRequest("Maria Relatório Silva"));
        LoanResponse loan = loanService.registerLoan(LoanCreateRequest.builder().beneficiaryId(b.id())
                .equipmentId(equipment()).loanDate(today.minusDays(35)).retroactiveJustification("Papel").build());
        loanService.registerDigitalSignature(loan.getId(), new DigitalSignatureRequest(null));

        Long repaired = equipment();
        Long maintenanceId = maintenanceService.open(new MaintenanceCreateRequest(repaired, null, "Rodinha quebrada",
                null, "Oficina Central")).id();
        maintenanceService.addCost(maintenanceId, new MaintenanceCostRequest(CostType.PECA, "Rodinha", "Rodinha 5\"",
                new BigDecimal("80.00"), null, null));

        Long scrapped = equipment();
        equipmentService.writeOff(scrapped, new WriteOffRequest(WriteOffReason.INSERVIVEL, "Estrutura trincada"));
        equipment();
        SecurityContextHolder.clearContext();
        return new Data(b, loan);
    }

    private String dashboard(RequestPostProcessor who) throws Exception {
        return mockMvc.perform(get("/api/v1/dashboard").param("tipo", TYPE.name()).with(who))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static long num(String json, String path) {
        return ((Number) JsonPath.read(json, path)).longValue();
    }

    @Test
    void deveCalcularIndicadoresEOcultarListasQuandoPerfilConsulta() throws Exception {
        String before = dashboard(ADMIN);
        Data data = seed();
        String after = dashboard(ADMIN);

        assertThat(num(after, "$.occupancy.activeEquipments") - num(before, "$.occupancy.activeEquipments"))
                .isEqualTo(3); // baixado não conta (RN54/RN55)
        assertThat(num(after, "$.occupancy.writtenOff") - num(before, "$.occupancy.writtenOff")).isEqualTo(1);
        assertThat(num(after, "$.occupancy.loaned") - num(before, "$.occupancy.loaned")).isEqualTo(1);
        assertThat(num(after, "$.overdueLoans") - num(before, "$.overdueLoans")).isEqualTo(1);
        assertThat(new BigDecimal(JsonPath.read(after, "$.maintenanceCost").toString())
                .subtract(new BigDecimal(JsonPath.read(before, "$.maintenanceCost").toString())))
                .isEqualByComparingTo("80.00");
        assertThat(after).contains("\"personalDataHidden\":false");
        List<Integer> overdueIds = JsonPath.read(after, "$.overdueItems[*].loanId");
        assertThat(overdueIds).contains(data.overdueLoan().getId().intValue());
        assertThat(JsonPath.<List<String>>read(after, "$.byType[*].type")).containsExactly(TYPE.name());

        String viewer = dashboard(VIEWER);
        assertThat(viewer).contains("\"personalDataHidden\":true").doesNotContain("Maria");
        assertThat((Object) JsonPath.read(viewer, "$.overdueItems")).isNull();
        assertThat(num(viewer, "$.overdueLoans")).isEqualTo(num(after, "$.overdueLoans"));
    }

    @Test
    void deveExportarCsvCompletoParaAdminEMascaradoParaConsulta() throws Exception {
        Data data = seed();
        String start = today.minusDays(40).toString();

        MvcResult admin = mockMvc.perform(get("/api/v1/relatorios/EMPRESTIMOS").param("formato", "CSV")
                        .param("inicio", start).param("tipoEquipamento", TYPE.name()).with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("relatorio-emprestimos-")))
                .andReturn();
        String adminCsv = new String(admin.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(adminCsv).startsWith("﻿Nº;Retirada;").contains("Maria Relatório Silva")
                .contains(data.beneficiary().cpf().substring(0, 3) + ".");

        MvcResult viewer = mockMvc.perform(get("/api/v1/relatorios/EMPRESTIMOS").param("inicio", start)
                        .param("tipoEquipamento", TYPE.name()).with(VIEWER))
                .andExpect(status().isOk()).andReturn();
        String viewerCsv = new String(viewer.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(viewerCsv).contains("Maria R. S.").doesNotContain("Maria Relatório Silva")
                .contains("***.").doesNotContain(data.beneficiary().cpf());

        List<AuditLog> exports = auditLogRepository.search("Report", "EMPRESTIMOS", null, null).stream()
                .filter(a -> a.getAction() == AuditAction.EXPORT).toList();
        assertThat(exports).extracting(AuditLog::getUsername).contains("admin.teste", "consulta.teste");
        assertThat(exports).anySatisfy(a -> assertThat(a.getDetails()).contains("\"personalDataMasked\":true")
                .contains(start));
    }

    @Test
    void deveGerarPdfEInventarioComBaixadosEManutencoes() throws Exception {
        seed();
        MvcResult pdf = mockMvc.perform(get("/api/v1/relatorios/INVENTARIO").param("formato", "PDF")
                        .param("tipoEquipamento", TYPE.name()).with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn();
        assertThat(new String(pdf.getResponse().getContentAsByteArray(), 0, 5, StandardCharsets.ISO_8859_1))
                .isEqualTo("%PDF-");

        String inventory = new String(mockMvc.perform(get("/api/v1/relatorios/INVENTARIO")
                        .param("tipoEquipamento", TYPE.name()).with(VIEWER))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(inventory).contains("Baixado").contains("Inservível").contains("80,00");

        String maintenances = new String(mockMvc.perform(get("/api/v1/relatorios/MANUTENCOES").with(ADMIN))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(maintenances).contains("Rodinha quebrada").contains("Oficina Central").contains("Rodinha 5");

        String beneficiaries = new String(mockMvc.perform(get("/api/v1/relatorios/ASSISTIDOS")
                        .param("inicio", today.minusDays(40).toString()).with(ADMIN))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(beneficiaries).contains("Maria Relatório Silva").contains("60–79");
    }

    @Test
    void deveRecusarPeriodoInvalidoEPerfilSemPermissao() throws Exception {
        mockMvc.perform(get("/api/v1/relatorios/EMPRESTIMOS").param("inicio", today.toString())
                        .param("fim", today.minusDays(1).toString()).with(ADMIN))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/v1/dashboard").param("inicio", today.toString())
                        .param("fim", today.minusDays(1).toString()).with(ADMIN))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/v1/relatorios/EMPRESTIMOS")).andExpect(status().isUnauthorized());
    }
}
