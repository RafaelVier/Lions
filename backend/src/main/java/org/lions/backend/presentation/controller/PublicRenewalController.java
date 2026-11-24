package org.lions.backend.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.request.PublicRenewalRequest;
import org.lions.backend.application.dto.response.PublicRenewalResponse;
import org.lions.backend.application.service.loan.LoanService;
import org.lions.backend.application.support.AttemptLimiter;
import org.lions.backend.application.support.CpfValidator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rota pública de renovação pelo beneficiário (UC16), com limite de tentativas por IP e por CPF. */
@RestController
@RequestMapping("/api/v1/emprestimos/renovacao-publica")
@RequiredArgsConstructor
@Tag(name = "Renovação pública", description = "Renovação do empréstimo pelo próprio beneficiário, sem login (UC16, RN23)")
public class PublicRenewalController {

    private final LoanService loanService;
    private final AttemptLimiter attemptLimiter;

    @PostMapping
    @Operation(summary = "Renova o empréstimo informando CPF, data de nascimento e número de patrimônio",
            description = "Aplica as regras da RN23. Dados que não conferem → 422 com mensagem genérica; "
                    + "excesso de tentativas → 429.")
    public PublicRenewalResponse renew(@Valid @RequestBody PublicRenewalRequest request,
                                       HttpServletRequest httpRequest) {
        attemptLimiter.acquire("renovacao-ip:" + httpRequest.getRemoteAddr(),
                "renovacao-cpf:" + CpfValidator.normalize(request.cpf()));
        return loanService.renewByBeneficiary(request);
    }
}
