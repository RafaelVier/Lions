package org.lions.backend.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lions.backend.application.dto.request.BeneficiaryRequest;
import org.lions.backend.support.TestFixtures;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * I.2 — teste de integração ponta a ponta, só pela API HTTP (MockMvc) e com autenticação JWT real.
 * <p>
 * Sem {@code @Transactional} de propósito: cada requisição faz commit, como em produção, e os eventos entre módulos
 * (avaria → manutenção, item disponível → reserva para a fila) e a sincronização (uma transação por operação) rodam
 * exatamente como no servidor. Por isso o teste usa dados próprios (nomes e CPFs aleatórios), não depende de
 * contagens absolutas do banco e não deixa ninguém aguardando na fila ao terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FluxoPontaAPontaIT {

    private static final String API = "/api/v1";
    /** Tipo do fluxo principal (pouco usado pelos demais testes). */
    private static final String MAIN_TYPE = "OUTROS";
    /** Tipo dos cenários de renovação pública e sincronização. */
    private static final String OTHER_TYPE = "MULETAS";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private final String suffix = Integer.toString(ThreadLocalRandom.current().nextInt(100_000, 999_999));
    private String admin;
    private String operator;

    @BeforeEach
    void login() throws Exception {
        admin = login("marcio", "Lions@123");
        operator = createUserAndLogin("operador.e2e." + suffix, "ROLE_OPERATOR");
    }

    // ------------------------------------------------------------------------------------------------------------
    // Fluxo principal: pedido público → aprovação → termo → assinatura → devolução com avaria → manutenção → APTO
    // (Admin) → reserva automática para o 1º da fila
    // ------------------------------------------------------------------------------------------------------------

    @Test
    void deveExecutarFluxoCompletoQuandoPedidoPublicoAteReservaParaFila() throws Exception {
        String nameA = "Ana Ponta " + suffix;
        String nameB = "Bruno Fila " + suffix;

        // 1. Pedido público (sem token) — a resposta é só o protocolo
        long orderA = publicOrder(nameA);
        String orderJson = body(mockMvc.perform(auth(get(API + "/pedidos/" + orderA), operator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE_ANALISE"))
                .andExpect(jsonPath("$.assistedRegistration").value(false)));
        long beneficiaryA = id(orderJson, "$.beneficiaryId");

        // 2. Cadastro do item e aprovação → empréstimo pendente de assinatura com termo gerado
        long equipment = createEquipment(MAIN_TYPE);
        String patrimony = JsonPath.read(body(mockMvc.perform(auth(get(API + "/equipamentos/" + equipment), operator))
                .andExpect(jsonPath("$.status").value("DISPONIVEL"))), "$.patrimonyNumber");

        String approval = body(mockMvc.perform(auth(post(API + "/pedidos/" + orderA + "/aprovar"), operator)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("equipmentId", equipment))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.status").value("APROVADO"))
                .andExpect(jsonPath("$.loan.status").value("PENDENTE_ASSINATURA"))
                .andExpect(jsonPath("$.loan.termAvailable").value(true))
                .andExpect(jsonPath("$.loan.termHashSha256").isNotEmpty()));
        long loanA = id(approval, "$.loan.id");
        expectEquipmentStatus(equipment, "EMPRESTADO");

        // 3. Termo em PDF (RN21/RN61)
        byte[] term = mockMvc.perform(auth(get(API + "/emprestimos/" + loanA + "/termo"), operator))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(term, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");

        // 4. Assinatura digital → ATIVO
        mockMvc.perform(auth(post(API + "/emprestimos/" + loanA + "/assinatura-digital"), operator)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.signatureType").value("DIGITAL"));

        // 5. Sem estoque: segundo pedido público do mesmo tipo vai para a fila (nenhum item do tipo disponível)
        long orderB = publicOrder(nameB);
        String queued = body(mockMvc.perform(auth(post(API + "/pedidos/" + orderB + "/fila"), operator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.status").value("EM_FILA_ESPERA"))
                .andExpect(jsonPath("$.waitlistEntry.status").value("AGUARDANDO")));
        long entryB = id(queued, "$.waitlistEntry.id");
        long beneficiaryB = id(queued, "$.waitlistEntry.beneficiaryId");

        // 6. Devolução com avaria → manutenção corretiva automática (RN24) + ocorrência DANO (RN40)
        mockMvc.perform(auth(post(API + "/emprestimos/" + loanA + "/devolucao"), operator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("returnCondition", "DANIFICADO", "damageDescription", "Roda quebrada"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEVOLVIDO"));
        expectEquipmentStatus(equipment, "EM_MANUTENCAO");

        String maintenances = body(mockMvc.perform(auth(get(API + "/manutencoes")
                        .param("equipamentoId", Long.toString(equipment)), operator))
                .andExpect(status().isOk()));
        List<Integer> openFromLoan = JsonPath.read(maintenances,
                "$[?(@.originLoanId == " + loanA + " && @.status == 'EM_ANDAMENTO')].id");
        assertThat(openFromLoan).hasSize(1);
        long maintenance = openFromLoan.get(0);
        assertThat(JsonPath.<List<String>>read(maintenances, "$[?(@.id == " + maintenance + ")].type"))
                .containsExactly("CORRETIVA");

        String occurrences = body(mockMvc.perform(auth(get(API + "/ocorrencias")
                        .param("beneficiarioId", Long.toString(beneficiaryA)), operator))
                .andExpect(status().isOk()));
        assertThat(JsonPath.<List<String>>read(occurrences, "$[?(@.loanId == " + loanA + ")].type"))
                .containsExactly("DANO");
        mockMvc.perform(auth(get(API + "/beneficiarios/" + beneficiaryA + "/situacao"), operator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(true)); // RN20

        // 7. Custo e encerramento: só o Admin encerra (UC23); APTO → item disponível → reserva para o 1º da fila (RN32)
        mockMvc.perform(auth(post(API + "/manutencoes/" + maintenance + "/custos"), operator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("type", "PECA", "description", "Roda nova", "amount", 85.50))))
                .andExpect(status().isOk());
        mockMvc.perform(auth(post(API + "/manutencoes/" + maintenance + "/encerrar"), operator)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("verdict", "APTO"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(auth(post(API + "/manutencoes/" + maintenance + "/encerrar"), admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("verdict", "APTO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA_APTO"))
                .andExpect(jsonPath("$.totalCost").value(85.5));

        expectEquipmentStatus(equipment, "RESERVADO");
        mockMvc.perform(auth(get(API + "/fila/" + entryB), operator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVADO"))
                .andExpect(jsonPath("$.beneficiaryId").value(beneficiaryB))
                .andExpect(jsonPath("$.reservedEquipmentId").value(equipment))
                .andExpect(jsonPath("$.reservedPatrimonyNumber").value(patrimony))
                .andExpect(jsonPath("$.reservationExpiresAt").isNotEmpty());

        // a reserva gera aviso ao beneficiário (envio automático ou contato manual — RN47)
        String notifications = body(mockMvc.perform(auth(get(API + "/notificacoes")
                        .param("beneficiarioId", Long.toString(beneficiaryB)), operator))
                .andExpect(status().isOk()));
        assertThat(JsonPath.<List<Object>>read(notifications, "$[*]")).isNotEmpty();

        // 8. Aprovação do pedido em fila com o item reservado → posição ATENDIDA (ninguém fica aguardando)
        mockMvc.perform(auth(post(API + "/pedidos/" + orderB + "/aprovar"), operator)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("equipmentId", equipment))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loan.status").value("PENDENTE_ASSINATURA"))
                .andExpect(jsonPath("$.loan.beneficiaryId").value(beneficiaryB));
        mockMvc.perform(auth(get(API + "/fila/" + entryB), operator))
                .andExpect(jsonPath("$.status").value("ATENDIDO"));
        expectEquipmentStatus(equipment, "EMPRESTADO");

        // 9. Histórico do equipamento consolida empréstimos e manutenção (custo acumulado)
        mockMvc.perform(auth(get(API + "/equipamentos/" + equipment + "/historico"), operator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loans.length()").value(2))
                .andExpect(jsonPath("$.maintenances.length()").value(1))
                .andExpect(jsonPath("$.totalMaintenanceCost").value(85.5));

        // 10. Dashboard (RF15) e relatório CSV (RF11) refletem o fluxo, com visão restrita para Consulta
        String viewer = createUserAndLogin("consulta.e2e." + suffix, "ROLE_VIEWER");
        mockMvc.perform(auth(get(API + "/dashboard").param("tipo", MAIN_TYPE), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipmentType").value(MAIN_TYPE))
                .andExpect(jsonPath("$.personalDataHidden").value(false))
                .andExpect(jsonPath("$.occupancy.loaned").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.loanVolume.loans").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.loanVolume.returns").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.maintenanceCost").value(org.hamcrest.Matchers.greaterThanOrEqualTo(85.5)))
                .andExpect(jsonPath("$.overdueItems").isArray());
        mockMvc.perform(auth(get(API + "/dashboard").param("tipo", MAIN_TYPE), viewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personalDataHidden").value(true))
                .andExpect(jsonPath("$.overdueItems").doesNotExist());

        String adminCsv = csv("EMPRESTIMOS", admin);
        assertThat(adminCsv).startsWith("﻿").contains(patrimony).contains(nameA).contains(nameB);
        String viewerCsv = csv("EMPRESTIMOS", viewer);
        assertThat(viewerCsv).contains(patrimony).doesNotContain(nameA).doesNotContain(nameB);

        String exports = body(mockMvc.perform(auth(get(API + "/auditoria"), admin)).andExpect(status().isOk()));
        assertThat(JsonPath.<List<String>>read(exports, "$[?(@.action == 'EXPORT')].username"))
                .contains("marcio", "consulta.e2e." + suffix); // RN44
    }

    // ------------------------------------------------------------------------------------------------------------
    // UC16 — renovação pública pelo beneficiário (sem login)
    // ------------------------------------------------------------------------------------------------------------

    @Test
    void deveRenovarPelaRotaPublicaQuandoBeneficiarioConfereDados() throws Exception {
        BeneficiaryRequest request = TestFixtures.beneficiaryRequest("Carla Renova " + suffix);
        long beneficiary = createBeneficiary(request);
        long equipment = createEquipment(OTHER_TYPE);
        String patrimony = JsonPath.read(body(mockMvc.perform(auth(get(API + "/equipamentos/" + equipment), admin))),
                "$.patrimonyNumber");

        // empréstimo retirado há 25 dias (retroativo: Admin + justificativa) → vence em 5 dias (prazo padrão 30)
        LocalDate loanDate = LocalDate.now().minusDays(25);
        Map<String, Object> loan = new LinkedHashMap<>();
        loan.put("beneficiaryId", beneficiary);
        loan.put("equipmentId", equipment);
        loan.put("loanDate", loanDate.toString());
        loan.put("retroactiveJustification", "Registrado no papel durante o mutirão");
        String created = body(mockMvc.perform(auth(post(API + "/emprestimos"), admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(loan)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.retroactive").value(true)));
        long loanId = id(created, "$.id");
        LocalDate due = LocalDate.parse(JsonPath.read(created, "$.expectedReturnDate"));
        mockMvc.perform(auth(post(API + "/emprestimos/" + loanId + "/assinatura-digital"), operator)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(jsonPath("$.status").value("ATIVO"));

        String ip = "10.9." + ThreadLocalRandom.current().nextInt(250) + "." + ThreadLocalRandom.current().nextInt(250);
        Map<String, Object> wrong = Map.of("cpf", request.cpf(), "birthDate", request.birthDate().plusDays(1).toString(),
                "patrimonyNumber", patrimony);
        mockMvc.perform(publicRenewal(wrong, ip)).andExpect(status().isUnprocessableEntity());

        Map<String, Object> right = Map.of("cpf", request.cpf(), "birthDate", request.birthDate().toString(),
                "patrimonyNumber", patrimony);
        mockMvc.perform(publicRenewal(right, ip))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patrimonyNumber").value(patrimony))
                .andExpect(jsonPath("$.previousExpectedReturnDate").value(due.toString()))
                .andExpect(jsonPath("$.expectedReturnDate").value(due.plusDays(30).toString()))
                .andExpect(jsonPath("$.renewalCount").value(1))
                .andExpect(jsonPath("$.maxRenewals").value(2))
                .andExpect(jsonPath("$.cpf").doesNotExist());

        // fora da janela de 7 dias agora (RN23) → recusa com o motivo
        mockMvc.perform(publicRenewal(right, ip)).andExpect(status().isUnprocessableEntity());

        mockMvc.perform(auth(get(API + "/emprestimos/" + loanId), operator))
                .andExpect(jsonPath("$.renewalCount").value(1))
                .andExpect(jsonPath("$.expectedReturnDate").value(due.plusDays(30).toString()))
                .andExpect(jsonPath("$.renewals[0].renewedBy").value("BENEFICIARIO"));

        // devolve em bom estado para não deixar empréstimo ativo para os demais testes
        mockMvc.perform(auth(post(API + "/emprestimos/" + loanId + "/devolucao"), operator)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("returnCondition", "BOM_ESTADO"))))
                .andExpect(status().isOk());
        expectEquipmentStatus(equipment, "DISPONIVEL");
    }

    // ------------------------------------------------------------------------------------------------------------
    // RN60 — sincronização offline com conflito
    // ------------------------------------------------------------------------------------------------------------

    @Test
    void deveRegistrarConflitoQuandoSincronizaRetiradaDeItemJaEmprestado() throws Exception {
        long first = createBeneficiary(TestFixtures.beneficiaryRequest("Davi Offline " + suffix));
        long second = createBeneficiary(TestFixtures.beneficiaryRequest("Elisa Offline " + suffix));
        long equipment = createEquipment(OTHER_TYPE);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        String op1 = "e2e-" + UUID.randomUUID();
        String op2 = "e2e-" + UUID.randomUUID();

        // dois dispositivos retiraram o mesmo item sem conexão; o lote chega fora de ordem
        String result = body(mockMvc.perform(auth(post(API + "/sincronizacao"), operator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("operations", List.of(
                                checkout(op2, now.minusHours(1), second, equipment),
                                checkout(op1, now.minusHours(3), first, equipment))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)));

        // aplicado em ordem de occurredAt: a mais antiga é aplicada, a outra vira conflito
        assertThat(JsonPath.<String>read(result, "$[0].clientOperationId")).isEqualTo(op1);
        assertThat(JsonPath.<String>read(result, "$[0].status")).isEqualTo("APLICADA");
        long loanId = id(result, "$[0].loanId");
        assertThat(JsonPath.<String>read(result, "$[1].status")).isEqualTo("CONFLITO");
        long conflictId = id(result, "$[1].conflictId");

        mockMvc.perform(auth(get(API + "/emprestimos/" + loanId), operator))
                .andExpect(jsonPath("$.status").value("PENDENTE_ASSINATURA")) // RN21
                .andExpect(jsonPath("$.beneficiaryId").value(first))
                .andExpect(jsonPath("$.loanDate").value(now.minusHours(3).toLocalDate().toString()));

        // reenvio da mesma operação não duplica (idempotência)
        mockMvc.perform(auth(post(API + "/sincronizacao"), operator).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("operations", List.of(checkout(op1, now.minusHours(3), first, equipment))))))
                .andExpect(jsonPath("$[0].status").value("DUPLICADA"))
                .andExpect(jsonPath("$[0].loanId").value(loanId));

        // conflito pendente para conferência: prevalece o registro existente (mais antigo)
        mockMvc.perform(auth(get(API + "/sincronizacao/conflitos/" + conflictId), operator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.prevailing").value("EXISTENTE"))
                .andExpect(jsonPath("$.equipmentId").value(equipment))
                .andExpect(jsonPath("$.existingLoanId").value(loanId));
        String pending = body(mockMvc.perform(auth(get(API + "/sincronizacao/conflitos")
                .param("status", "PENDENTE"), admin)));
        assertThat(JsonPath.<List<Integer>>read(pending, "$[*].id")).contains((int) conflictId);

        // só o Admin resolve
        mockMvc.perform(auth(patch(API + "/sincronizacao/conflitos/" + conflictId + "/resolver"), operator)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("note", "Conferido"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(auth(patch(API + "/sincronizacao/conflitos/" + conflictId + "/resolver"), admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("note", "Item ficou com o primeiro beneficiário"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVIDO"))
                .andExpect(jsonPath("$.resolvedBy").value("marcio"));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------------------------------------------------

    private String login(String username, String password) throws Exception {
        String response = body(mockMvc.perform(post(API + "/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", password))))
                .andExpect(status().isOk()));
        return JsonPath.read(response, "$.token");
    }

    private String createUserAndLogin(String username, String role) throws Exception {
        String password = "Senha@" + suffix;
        mockMvc.perform(auth(post(API + "/usuarios"), admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "fullName", "Usuário " + username,
                                "email", username + "@teste.lions", "password", password, "role", role))))
                .andExpect(status().isCreated());
        return login(username, password);
    }

    private long publicOrder(String name) throws Exception {
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("forSelf", false);
        order.put("requesterName", "Solicitante de " + name);
        order.put("requesterRelationship", "Filha");
        order.put("beneficiary", TestFixtures.beneficiaryRequest(name));
        order.put("equipmentType", MAIN_TYPE);
        order.put("needReason", "Pós-operatório");
        String response = body(mockMvc.perform(post(API + "/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content(json(order)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE_ANALISE"))
                .andExpect(jsonPath("$.beneficiaryId").doesNotExist()));
        return id(response, "$.id");
    }

    private long createBeneficiary(BeneficiaryRequest request) throws Exception {
        return id(body(mockMvc.perform(auth(post(API + "/beneficiarios"), operator)
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isCreated())), "$.id");
    }

    private long createEquipment(String type) throws Exception {
        return id(body(mockMvc.perform(auth(post(API + "/equipamentos"), operator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("type", type, "condition", "BOM"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patrimonyNumber").value(org.hamcrest.Matchers.startsWith("PT-")))), "$.id");
    }

    private void expectEquipmentStatus(long equipment, String expected) throws Exception {
        mockMvc.perform(auth(get(API + "/equipamentos/" + equipment), operator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(expected));
    }

    private String csv(String report, String token) throws Exception {
        byte[] bytes = mockMvc.perform(auth(get(API + "/relatorios/" + report), token)
                        .param("formato", "CSV").param("tipoEquipamento", MAIN_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andReturn().getResponse().getContentAsByteArray();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private MockHttpServletRequestBuilder publicRenewal(Map<String, Object> body, String ip) throws Exception {
        return post(API + "/emprestimos/renovacao-publica").contentType(MediaType.APPLICATION_JSON).content(json(body))
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                });
    }

    private static Map<String, Object> checkout(String id, LocalDateTime at, long beneficiary, long equipment) {
        return Map.of("clientOperationId", id, "type", "RETIRADA", "occurredAt", at.toString(),
                "beneficiaryId", beneficiary, "equipmentId", equipment);
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static long id(String json, String path) {
        return ((Number) JsonPath.read(json, path)).longValue();
    }
}
