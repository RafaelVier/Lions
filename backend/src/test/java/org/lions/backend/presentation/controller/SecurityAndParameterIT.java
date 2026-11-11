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
import org.lions.backend.infrastructure.security.TokenRevocationService;
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
    private TokenRevocationService revocationService;
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
        // Rotas públicas ainda sem controller (Onda 1) não podem responder 401/403.
        mockMvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/ajuda/publico")).andExpect(status().isNotFound());
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

        mockMvc.perform(get(PARAMS).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        revocationService.revoke(jwtTokenProvider.getJti(token), jwtTokenProvider.getExpiration(token));

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
                .andExpect(jsonPath("$.length()").value(17))
                .andExpect(jsonPath("$[*].key", org.hamcrest.Matchers.hasItem("loan.max-renewals")))
                .andExpect(jsonPath("$[*].key", org.hamcrest.Matchers.hasItem("loan.days.CADEIRA_DE_RODAS")))
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
}
