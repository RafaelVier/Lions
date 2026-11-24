package org.lions.backend.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.request.AdjustmentRequest;
import org.lions.backend.application.dto.request.DigitalSignatureRequest;
import org.lions.backend.application.dto.request.LoanCreateRequest;
import org.lions.backend.application.dto.request.LoanReturnRequest;
import org.lions.backend.application.dto.request.ReasonRequest;
import org.lions.backend.application.dto.response.AdjustmentResponse;
import org.lions.backend.application.dto.response.LoanResponse;
import org.lions.backend.application.service.loan.LoanService;
import org.lions.backend.application.service.loan.LoanService.LoanFilter;
import org.lions.backend.application.service.loan.LoanService.StoredFile;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.LoanStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/emprestimos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
@Tag(name = "Empréstimos", description = "Empréstimo, termo, assinatura, renovação, devolução e ajustes (RF05, RF07, RF10)")
@SecurityRequirement(name = "bearerAuth")
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra empréstimo (nasce PENDENTE_ASSINATURA; retroativo só Admin — RN22)")
    public LoanResponse create(@Valid @RequestBody LoanCreateRequest request) {
        return loanService.registerLoan(request);
    }

    @GetMapping
    @Operation(summary = "Histórico de empréstimos com filtros (RF10)")
    public List<LoanResponse> findAll(
            @RequestParam(name = "status", required = false) LoanStatus status,
            @RequestParam(name = "beneficiarioId", required = false) Long beneficiaryId,
            @RequestParam(name = "equipamentoId", required = false) Long equipmentId,
            @RequestParam(name = "inicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate start,
            @RequestParam(name = "fim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate end,
            @RequestParam(name = "tipo", required = false) EquipmentType equipmentType) {
        return loanService.findAll(new LoanFilter(status, beneficiaryId, equipmentId, start, end, equipmentType));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha o empréstimo com renovações, devolução, ajustes e link de lembrete")
    public LoanResponse findById(@PathVariable Long id) {
        return loanService.findById(id);
    }

    @GetMapping("/{id}/termo")
    @Operation(summary = "PDF do termo de responsabilidade armazenado (RN61)")
    public ResponseEntity<byte[]> term(@PathVariable Long id) {
        return file(loanService.getTerm(id));
    }

    @GetMapping("/{id}/termo-assinado")
    @Operation(summary = "Termo assinado digitalizado (PDF, JPG ou PNG)")
    public ResponseEntity<byte[]> signedTerm(@PathVariable Long id) {
        return file(loanService.getSignedTerm(id));
    }

    @PostMapping("/{id}/assinatura-digital")
    @Operation(summary = "Registra a assinatura digital do termo (empréstimo passa a ATIVO — RN21)")
    public LoanResponse digitalSignature(@PathVariable Long id,
                                         @Valid @RequestBody(required = false) DigitalSignatureRequest request) {
        return loanService.registerDigitalSignature(id, request);
    }

    @PostMapping(value = "/{id}/assinatura-fisica", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Registra a assinatura física enviando o termo digitalizado (PDF/JPG/PNG até 10 MB)")
    public LoanResponse physicalSignature(@PathVariable Long id,
                                          @RequestPart("file") MultipartFile file,
                                          @RequestParam(name = "signerName", required = false) String signerName) {
        return loanService.registerPhysicalSignature(id, signerName, bytes(file), file.getOriginalFilename(),
                file.getContentType());
    }

    @PostMapping("/{id}/renovacao")
    @Operation(summary = "Renova o empréstimo (RN23)")
    public LoanResponse renew(@PathVariable Long id) {
        return loanService.renew(id);
    }

    @PostMapping("/{id}/devolucao")
    @Operation(summary = "Registra a devolução com vistoria (avaria abre manutenção — RN24, RN40)")
    public LoanResponse registerReturn(@PathVariable Long id, @Valid @RequestBody LoanReturnRequest request) {
        return loanService.registerReturn(id, request);
    }

    @PostMapping("/{id}/cancelamento")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancela o empréstimo com motivo (RN26)")
    public LoanResponse cancel(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        return loanService.cancel(id, request.reason());
    }

    @PostMapping("/{id}/ajustes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registra ajuste justificado no empréstimo (RN41)")
    public AdjustmentResponse addAdjustment(@PathVariable Long id, @Valid @RequestBody AdjustmentRequest request) {
        return loanService.addAdjustment(id, request);
    }

    @GetMapping("/{id}/ajustes")
    @Operation(summary = "Lista os ajustes registrados no empréstimo")
    public List<AdjustmentResponse> findAdjustments(@PathVariable Long id) {
        return loanService.findAdjustments(id);
    }

    private static ResponseEntity<byte[]> file(StoredFile stored) {
        return FileResponses.inline(stored.content(), stored.contentType(), stored.fileName());
    }

    private static byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o arquivo enviado.", e);
        }
    }
}
