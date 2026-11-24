package org.lions.backend.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.request.BeneficiaryRequest;
import org.lions.backend.application.dto.request.ConsentRequest;
import org.lions.backend.application.dto.request.ReasonRequest;
import org.lions.backend.application.dto.response.BeneficiaryHistoryResponse;
import org.lions.backend.application.dto.response.BeneficiaryResponse;
import org.lions.backend.application.dto.response.BeneficiaryRestriction;
import org.lions.backend.application.dto.response.BeneficiarySummaryResponse;
import org.lions.backend.application.service.beneficiary.BeneficiaryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/beneficiarios")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
@Tag(name = "Beneficiários", description = "Cadastro, consentimento LGPD e histórico do assistido (RF02, RN03–RN08, RN42, RN59)")
@SecurityRequirement(name = "bearerAuth")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra beneficiário (409 com existingId se o documento já estiver ativo)")
    public BeneficiaryResponse create(@Valid @RequestBody BeneficiaryRequest request) {
        return beneficiaryService.create(request);
    }

    @GetMapping
    @Operation(summary = "Lista beneficiários (documento mascarado), com filtros por nome, documento e situação")
    public List<BeneficiarySummaryResponse> findAll(@RequestParam(name = "nome", required = false) String name,
                                                    @RequestParam(name = "documento", required = false) String document,
                                                    @RequestParam(name = "ativo", required = false) Boolean active) {
        return beneficiaryService.findAll(name, document, active);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um beneficiário")
    public BeneficiaryResponse findById(@PathVariable Long id) {
        return beneficiaryService.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza o cadastro (o consentimento é alterado em rota própria)")
    public BeneficiaryResponse update(@PathVariable Long id, @Valid @RequestBody BeneficiaryRequest request) {
        return beneficiaryService.update(id, request);
    }

    @PutMapping("/{id}/consentimento")
    @Operation(summary = "Registra o consentimento LGPD e os canais de notificação autorizados")
    public BeneficiaryResponse updateConsent(@PathVariable Long id, @Valid @RequestBody ConsentRequest request) {
        return beneficiaryService.updateConsent(id, request);
    }

    @PatchMapping("/{id}/inativar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Inativa (422 se houver empréstimo, ocorrência aberta ou fila — RN08)")
    public BeneficiaryResponse inactivate(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        return beneficiaryService.inactivate(id, request.reason());
    }

    @PatchMapping("/{id}/reativar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reativa um beneficiário inativo")
    public BeneficiaryResponse reactivate(@PathVariable Long id) {
        return beneficiaryService.reactivate(id);
    }

    @PostMapping("/{id}/anonimizar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Anonimiza os dados pessoais (LGPD — RN59)")
    public BeneficiaryResponse anonymize(@PathVariable Long id) {
        return beneficiaryService.anonymize(id);
    }

    @GetMapping("/{id}/historico")
    @Operation(summary = "Histórico consolidado: empréstimos, solicitações, fila e ocorrências (RN42)")
    public BeneficiaryHistoryResponse history(@PathVariable Long id) {
        return beneficiaryService.history(id);
    }

    @GetMapping("/{id}/situacao")
    @Operation(summary = "Situação/restrições do beneficiário para novos empréstimos e fila (RN08, RN20)")
    public BeneficiaryRestriction restriction(@PathVariable Long id) {
        return beneficiaryService.restriction(id);
    }
}
