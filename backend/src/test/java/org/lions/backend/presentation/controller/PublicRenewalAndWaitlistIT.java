package org.lions.backend.presentation.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.lions.backend.application.dto.request.DigitalSignatureRequest;
import org.lions.backend.application.dto.request.EquipmentCreateRequest;
import org.lions.backend.application.dto.request.LoanCreateRequest;
import org.lions.backend.application.dto.response.BeneficiaryResponse;
import org.lions.backend.application.dto.response.EquipmentResponse;
import org.lions.backend.application.dto.response.LoanResponse;
import org.lions.backend.application.service.beneficiary.BeneficiaryService;
import org.lions.backend.application.service.equipment.EquipmentService;
import org.lions.backend.application.service.loan.LoanService;
import org.lions.backend.domain.entity.Beneficiary;
import org.lions.backend.domain.entity.WaitlistEntry;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.WaitlistStatus;
import org.lions.backend.domain.repository.BeneficiaryRepositoryPort;
import org.lions.backend.domain.repository.LoanRenewalRepositoryPort;
import org.lions.backend.domain.repository.WaitlistEntryRepositoryPort;
import org.lions.backend.support.TestFixtures;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** G.10 (UC16 — renovação pública) e G.7 (RN34 — termo pendente remove da fila). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicRenewalAndWaitlistIT {

    private static final String URL = "/api/v1/emprestimos/renovacao-publica";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private BeneficiaryService beneficiaryService;
    @Autowired
    private EquipmentService equipmentService;
    @Autowired
    private LoanService loanService;
    @Autowired
    private BeneficiaryRepositoryPort beneficiaryRepository;
    @Autowired
    private WaitlistEntryRepositoryPort waitlistRepository;
    @Autowired
    private LoanRenewalRepositoryPort renewalRepository;

    private final String ip = "10.0." + ThreadLocalRandom.current().nextInt(250) + "."
            + ThreadLocalRandom.current().nextInt(250);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private record Fixture(BeneficiaryResponse beneficiary, EquipmentResponse equipment, LoanResponse loan) {
    }

    /** Empréstimo ativo com vencimento em {@code daysToDue} dias (prazo padrão de 30). */
    private Fixture activeLoan(int daysToDue, EquipmentType type) {
        TestFixtures.authenticateAs("admin", "ADMIN");
        BeneficiaryResponse b = beneficiaryService.create(TestFixtures.beneficiaryRequest("Renova Teste"));
        EquipmentResponse e = equipmentService.create(EquipmentCreateRequest.builder()
                .type(type).condition(EquipmentCondition.BOM).build());
        LoanResponse loan = loanService.registerLoan(LoanCreateRequest.builder().beneficiaryId(b.id())
                .equipmentId(e.getId()).loanDate(LocalDate.now().plusDays(daysToDue).minusDays(30))
                .retroactiveJustification("Registro no papel").build());
        loanService.registerDigitalSignature(loan.getId(), new DigitalSignatureRequest(null));
        SecurityContextHolder.clearContext(); // a rota é pública
        return new Fixture(b, e, loan);
    }

    private MockHttpServletRequestBuilder renewal(String cpf, LocalDate birthDate, String patrimony) {
        return post(URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"cpf\":\"" + cpf + "\",\"birthDate\":\"" + birthDate + "\",\"patrimonyNumber\":\""
                        + patrimony + "\"}")
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                });
    }

    @Test
    void deveRenovarSemLoginQuandoDadosConferemEElegivel() throws Exception {
        Fixture f = activeLoan(5, EquipmentType.ANDADOR);
        LocalDate due = LocalDate.now().plusDays(5);

        mockMvc.perform(renewal(f.beneficiary().cpf(), f.beneficiary().birthDate(),
                        f.equipment().getPatrimonyNumber().toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previousExpectedReturnDate").value(due.toString()))
                .andExpect(jsonPath("$.expectedReturnDate").value(due.plusDays(30).toString()))
                .andExpect(jsonPath("$.renewalCount").value(1))
                .andExpect(jsonPath("$.cpf").doesNotExist());

        assertThat(renewalRepository.findByLoanId(f.loan().getId()))
                .singleElement().satisfies(r -> assertThat(r.getRenewedBy()).isEqualTo(LoanService.BENEFICIARY_AUTHOR));
    }

    @Test
    void deveRecusarComMensagemGenericaQuandoDadosNaoConferem() throws Exception {
        Fixture f = activeLoan(5, EquipmentType.ANDADOR);
        String generic = "Os dados informados não conferem com nenhum empréstimo em aberto.";

        mockMvc.perform(renewal(f.beneficiary().cpf(), f.beneficiary().birthDate().plusDays(1),
                        f.equipment().getPatrimonyNumber()))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.detail").value(generic));
        mockMvc.perform(renewal(TestFixtures.randomCpf(), f.beneficiary().birthDate(),
                        f.equipment().getPatrimonyNumber()))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.detail").value(generic));
        mockMvc.perform(renewal(f.beneficiary().cpf(), f.beneficiary().birthDate(), "PT-9999"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.detail").value(generic));
    }

    @Test
    void deveAplicarRegrasDaRn23QuandoForaDaJanela() throws Exception {
        Fixture f = activeLoan(20, EquipmentType.ANDADOR);
        mockMvc.perform(renewal(f.beneficiary().cpf(), f.beneficiary().birthDate(), f.equipment().getPatrimonyNumber()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("somente a partir de")));
    }

    @Test
    void deveLimitarTentativasQuandoExcedeLimite() throws Exception {
        Fixture f = activeLoan(5, EquipmentType.ANDADOR);
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(renewal(f.beneficiary().cpf(), LocalDate.of(1900, 1, 1), "PT-0000"))
                    .andExpect(status().isUnprocessableEntity());
        }
        mockMvc.perform(renewal(f.beneficiary().cpf(), f.beneficiary().birthDate(), f.equipment().getPatrimonyNumber()))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void deveRemoverDaFilaQuandoTermoFicaPendente() {
        TestFixtures.authenticateAs("operador", "OPERATOR");
        BeneficiaryResponse b = beneficiaryService.create(TestFixtures.beneficiaryRequest("Fila Teste"));
        Beneficiary beneficiary = beneficiaryRepository.findById(b.id()).orElseThrow();
        WaitlistEntry entry = waitlistRepository.save(WaitlistEntry.builder().beneficiary(beneficiary)
                .equipmentType(EquipmentType.CADEIRA_DE_BANHO).queuedAt(LocalDateTime.now())
                .originalRequestedAt(LocalDateTime.now()).createdBy("teste").build());
        EquipmentResponse walker = equipmentService.create(EquipmentCreateRequest.builder()
                .type(EquipmentType.ANDADOR).condition(EquipmentCondition.BOM).build());

        loanService.registerLoan(LoanCreateRequest.builder().beneficiaryId(b.id()).equipmentId(walker.getId()).build());

        WaitlistEntry after = waitlistRepository.findById(entry.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(WaitlistStatus.REMOVIDO);
        assertThat(after.getRemovedReason()).contains("pendente de assinatura");
    }
}
