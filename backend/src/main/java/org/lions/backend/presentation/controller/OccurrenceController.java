package org.lions.backend.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.request.OccurrenceResolveRequest;
import org.lions.backend.application.dto.response.OccurrenceResponse;
import org.lions.backend.application.service.loan.OccurrenceService;
import org.lions.backend.domain.enums.OccurrenceStatus;
import org.lions.backend.domain.enums.OccurrenceType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ocorrencias")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
@Tag(name = "Ocorrências", description = "Avarias, atrasos e inadimplência do beneficiário (RN24, RN27)")
@SecurityRequirement(name = "bearerAuth")
public class OccurrenceController {

    private final OccurrenceService occurrenceService;

    @GetMapping
    @Operation(summary = "Lista ocorrências com filtros por beneficiário, status e tipo")
    public List<OccurrenceResponse> findAll(@RequestParam(name = "beneficiarioId", required = false) Long beneficiaryId,
                                            @RequestParam(name = "status", required = false) OccurrenceStatus status,
                                            @RequestParam(name = "tipo", required = false) OccurrenceType type) {
        return occurrenceService.findAll(beneficiaryId, status, type);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha uma ocorrência")
    public OccurrenceResponse findById(@PathVariable Long id) {
        return occurrenceService.findById(id);
    }

    @PatchMapping("/{id}/resolver")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resolve a ocorrência com nota (libera o beneficiário se não houver outra pendência)")
    public OccurrenceResponse resolve(@PathVariable Long id, @Valid @RequestBody OccurrenceResolveRequest request) {
        return occurrenceService.resolve(id, request.note());
    }
}
