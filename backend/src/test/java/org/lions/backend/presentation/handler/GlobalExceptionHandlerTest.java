package org.lions.backend.presentation.handler;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lions.backend.domain.entity.Equipment;
import org.lions.backend.domain.exception.BusinessException;
import org.lions.backend.domain.exception.ConflictException;
import org.lions.backend.domain.exception.ResourceNotFoundException;
import org.lions.backend.domain.exception.UnauthorizedException;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    record Body(@NotBlank(message = "Nome é obrigatório.") String name, @Positive Integer amount) {
    }

    @RestController
    @RequestMapping("/fake")
    static class FakeController {

        @PostMapping("/validate")
        String validate(@Valid @RequestBody Body body) {
            return "ok";
        }

        @GetMapping("/param")
        String param(@RequestParam int n) {
            return "ok";
        }

        @GetMapping("/business")
        String business() {
            throw new BusinessException("Equipamento indisponível.");
        }

        @GetMapping("/not-found")
        String notFound() {
            throw new ResourceNotFoundException("Beneficiário não encontrado.");
        }

        @GetMapping("/conflict")
        String conflict() {
            throw new ConflictException("Beneficiário já cadastrado.", Map.of("existingId", 42));
        }

        @GetMapping("/optimistic")
        String optimistic() {
            throw new ObjectOptimisticLockingFailureException(Equipment.class, 1L);
        }

        @GetMapping("/unauthorized")
        String unauthorized() {
            throw new UnauthorizedException("Token inválido.");
        }

        @GetMapping("/denied")
        String denied() {
            throw new AccessDeniedException("x");
        }

        @GetMapping("/boom")
        String boom() {
            throw new IllegalStateException("segredo interno");
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FakeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void deveRetornar400ComErrorsQuandoBeanValidationFalha() throws Exception {
        mockMvc.perform(post("/fake/validate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"amount\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors.name").value("Nome é obrigatório."))
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    void deveRetornar400QuandoCorpoMalformadoOuParametroInvalido() throws Exception {
        mockMvc.perform(post("/fake/validate").contentType(MediaType.APPLICATION_JSON).content("{nao json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.body").exists());
        mockMvc.perform(post("/fake/validate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"a\",\"amount\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
        mockMvc.perform(get("/fake/param").param("n", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.n").exists());
        mockMvc.perform(get("/fake/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.n").value("Parâmetro obrigatório."));
    }

    @Test
    void deveRetornar422QuandoRegraDeNegocioViolada() throws Exception {
        mockMvc.perform(get("/fake/business"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Equipamento indisponível."));
    }

    @Test
    void deveRetornar404QuandoRecursoNaoEncontrado() throws Exception {
        mockMvc.perform(get("/fake/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Beneficiário não encontrado."));
    }

    @Test
    void deveRetornar409ComDetailsQuandoConflito() throws Exception {
        mockMvc.perform(get("/fake/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Beneficiário já cadastrado."))
                .andExpect(jsonPath("$.existingId").value(42));
        mockMvc.perform(get("/fake/optimistic"))
                .andExpect(status().isConflict());
    }

    @Test
    void deveRetornar401E403QuandoSegurancaFalha() throws Exception {
        mockMvc.perform(get("/fake/unauthorized")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/fake/denied")).andExpect(status().isForbidden());
    }

    @Test
    void deveRetornar500SemVazarMensagemQuandoErroInesperado() throws Exception {
        mockMvc.perform(get("/fake/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("segredo"))));
    }
}
