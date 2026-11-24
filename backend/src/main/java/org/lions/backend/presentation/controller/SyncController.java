package org.lions.backend.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.request.SyncBatchRequest;
import org.lions.backend.application.dto.request.SyncConflictResolveRequest;
import org.lions.backend.application.dto.response.SyncConflictResponse;
import org.lions.backend.application.dto.response.SyncOperationResult;
import org.lions.backend.application.service.sync.SyncService;
import org.lions.backend.domain.enums.SyncConflictStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sincronizacao")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
@Tag(name = "Sincronização offline", description = "Lotes de operações feitas sem conexão e conflitos (RNF06, RN60)")
@SecurityRequirement(name = "bearerAuth")
public class SyncController {

    private final SyncService syncService;

    @PostMapping
    @Operation(summary = "Aplica um lote de operações offline (retirada, devolução, renovação)",
            description = "Cada operação traz clientOperationId (idempotência) e occurredAt (data/hora no dispositivo)."
                    + " Resultado por operação: APLICADA, RECUSADA, CONFLITO ou DUPLICADA.")
    public List<SyncOperationResult> sync(@Valid @RequestBody SyncBatchRequest request) {
        return syncService.process(request);
    }

    @GetMapping("/conflitos")
    @Operation(summary = "Lista conflitos de sincronização (filtro opcional por status)")
    public List<SyncConflictResponse> conflicts(
            @RequestParam(name = "status", required = false) SyncConflictStatus status) {
        return syncService.findConflicts(status);
    }

    @GetMapping("/conflitos/{id}")
    @Operation(summary = "Detalha um conflito de sincronização")
    public SyncConflictResponse conflict(@PathVariable Long id) {
        return syncService.findConflict(id);
    }

    @PatchMapping("/conflitos/{id}/resolver")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registra a conferência manual do conflito (somente Admin)")
    public SyncConflictResponse resolve(@PathVariable Long id, @Valid @RequestBody SyncConflictResolveRequest request) {
        return syncService.resolve(id, request.note());
    }
}
