package org.lions.backend.presentation.controller;

import org.junit.jupiter.api.Test;
import org.lions.backend.application.service.parameter.ParameterService;
import org.lions.backend.domain.entity.AuditLog;
import org.lions.backend.domain.entity.User;
import org.lions.backend.domain.enums.AuditAction;
import org.lions.backend.domain.enums.UserRole;
import org.lions.backend.domain.repository.AuditLogRepositoryPort;
import org.lions.backend.domain.repository.UserRepositoryPort;
import org.lions.backend.infrastructure.security.JwtTokenProvider;
import org.lions.backend.application.service.auth.SessionService;
import org.lions.backend.domain.enums.SessionEndReason;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityAndParameterIT {

    private static final String PARAMS = "/api/v1/parametros";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserRepositoryPort userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuditLogRepositoryPort auditLogRepository;

    // ---------- autenticação / autorização ----------

    @Test
    void deveRetornar401ProblemQuandoAnonimoAcessaRotaProtegida() throws Exception {
        mockMvc.perform(get(PARAMS))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
        mockMvc.perform(get("/api/v1/equipamentos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "visitante", roles = "VIEWER")
    void deveRetornar403QuandoPerfilSemPermissao() throws Exception {
        mockMvc.perform(get(PARAMS))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void devePermitirRotasPublicasQuandoAnonimo() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        // Rotas públicas respondem sem token: o pedido vazio cai na validação (400), não em 401/403.
        mockMvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/ajuda/publico")).andExpect(status().isOk());
    }

    @Test
    void deveResponderPreflightCorsQuandoOrigemPermitida() throws Exception {
        mockMvc.perform(options(PARAMS)
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT"))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void deveCriarAdminInicialQuandoAplicacaoSobe() {
        User admin = userRepository.findByUsername("marcio").orElseThrow();
        assertThat(admin.getRole()).isEqualTo(UserRole.ROLE_ADMIN);
        assertThat(admin.isActive()).isTrue();
        assertThat(admin.getEmail()).isEqualTo("marcio@lionsmedianeira.org.br");
        assertThat(passwordEncoder.matches("Lions@123", admin.getPassword())).isTrue();
    }

    @Test
    void deveAutenticarComJwtERejeitarQuandoTokenRevogado() throws Exception {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("marcio", "Lions@123"));
        String token = jwtTokenProvider.generateToken(auth);
        sessionService.start(jwtTokenProvider.getJti(token), "marcio", jwtTokenProvider.getExpiration(token));

        mockMvc.perform(get(PARAMS).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        sessionService.end(jwtTokenProvider.getJti(token), SessionEndReason.LOGOUT);

        mockMvc.perform(get(PARAMS).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(PARAMS).header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRejeitarLoginQuandoSenhaIncorreta() {
        assertThatThrownBy(() -> authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("marcio", "errada")))
                .isInstanceOf(org.springframework.security.core.AuthenticationException.class);
    }

    // ---------- ParameterController ----------

    @Test
    @WithMockUser(username = "admin.teste", roles = "ADMIN")
    void deveListarParametrosQuandoAdmin() throws Exception {
        mockMvc.perform(get(PARAMS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[*].key", org.hamcrest.Matchers.hasItem("loan.max-renewals")))
                .andExpect(jsonPath("$[*].key", org.hamcrest.Matchers.hasItem("loan.default-days")))
                .andExpect(jsonPath("$[?(@.key == 'loan.max-renewals')].value", org.hamcrest.Matchers.contains("2")));
    }

    @Test
    @WithMockUser(username = "admin.teste", roles = "ADMIN")
    void deveAtualizarEAuditarQuandoValorValido() throws Exception {
        mockMvc.perform(put(PARAMS + "/" + ParameterService.LOAN_MAX_RENEWALS)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"3\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("3"))
                .andExpect(jsonPath("$.customized").value(true))
                .andExpect(jsonPath("$.updatedBy").value("admin.teste"));

        List<AuditLog> logs = auditLogRepository.search(ParameterService.ENTITY_NAME,
                ParameterService.LOAN_MAX_RENEWALS, null, null);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getAction()).isEqualTo(AuditAction.UPDATE);
        assertThat(logs.get(0).getUsername()).isEqualTo("admin.teste");
    }

    @Test
    @WithMockUser(username = "admin.teste", roles = "ADMIN")
    void deveRetornarErroQuandoChaveOuValorInvalido() throws Exception {
        mockMvc.perform(put(PARAMS + "/chave.inexistente")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"3\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(put(PARAMS + "/" + ParameterService.LOAN_MAX_RENEWALS)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"0\"}"))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(put(PARAMS + "/" + ParameterService.LOAN_MAX_RENEWALS)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.value").exists());
    }

    @Test
    @WithMockUser(username = "operador", roles = "OPERATOR")
    void deveRetornar403QuandoOperadorAlteraParametro() throws Exception {
        mockMvc.perform(put(PARAMS + "/" + ParameterService.LOAN_MAX_RENEWALS)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"3\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin.teste", roles = "ADMIN")
    void devePermitirResumoDiarioQuandoAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/notificacoes/resumo-diario"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdueLoans").isArray())
                .andExpect(jsonPath("$.expiredReservations").isArray())
                .andExpect(jsonPath("$.openMaintenances").isArray())
                .andExpect(jsonPath("$.preventiveDue").isArray());
    }

    @Test
    @WithMockUser(username = "operador.teste", roles = "OPERATOR")
    void deveNegarResumoDiarioQuandoOperador() throws Exception {
        mockMvc.perform(get("/api/v1/notificacoes/resumo-diario")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "operador.teste", roles = "OPERATOR")
    void deveNegarEncerramentoDeManutencaoQuandoOperador() throws Exception {
        mockMvc.perform(post("/api/v1/manutencoes/1/encerrar")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"verdict\":\"APTO\"}"))
                .andExpect(status().isForbidden());
    }

    private String loginToken() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"marcio\",\"password\":\"Lions@123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.token");
    }

    @Test
    void deveRecusarTokenQuandoLogoutOuRenovado() throws Exception {
        String first = loginToken();
        String refreshed = com.jayway.jsonpath.JsonPath.read(mockMvc.perform(post("/api/v1/auth/refresh")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + first))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.token");

        // RN57 — o token anterior à renovação deixa de valer (sessão RENOVADA persistida)
        mockMvc.perform(get(PARAMS).header(HttpHeaders.AUTHORIZATION, "Bearer " + first))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(PARAMS).header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshed))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshed))
                .andExpect(status().is2xxSuccessful());
        mockMvc.perform(get(PARAMS).header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshed))
                .andExpect(status().isUnauthorized());
        assertThat(sessionService.isRevoked(jwtTokenProvider.getJti(refreshed))).isTrue();
    }

    @Test
    @WithMockUser(username = "operador.teste", roles = "OPERATOR")
    void deveValidarLoteDeSincronizacaoQuandoOperacaoFutura() throws Exception {
        String future = java.time.LocalDateTime.now().plusDays(1).withNano(0).toString();
        mockMvc.perform(post("/api/v1/sincronizacao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operations\":[{\"clientOperationId\":\"x1\",\"type\":\"RENOVACAO\","
                                + "\"occurredAt\":\"" + future + "\",\"loanId\":1}]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/sincronizacao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operations\":[]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/sincronizacao/conflitos")).andExpect(status().isOk());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/sincronizacao/conflitos/1/resolver")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"ok\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "consulta.teste", roles = "VIEWER")
    void deveNegarSincronizacaoQuandoPerfilConsulta() throws Exception {
        mockMvc.perform(post("/api/v1/sincronizacao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operations\":[{\"clientOperationId\":\"v1\",\"type\":\"RENOVACAO\","
                                + "\"occurredAt\":\"2025-01-01T10:00:00\",\"loanId\":1}]}"))
                .andExpect(status().isForbidden());
    }
}
