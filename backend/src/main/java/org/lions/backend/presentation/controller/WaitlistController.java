package org.lions.backend.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.request.ReasonRequest;
import org.lions.backend.application.dto.request.WaitlistEnqueueRequest;
import org.lions.backend.application.dto.request.WaitlistPriorityRequest;
import org.lions.backend.application.dto.response.WaitlistEntryResponse;
import org.lions.backend.application.service.waitlist.WaitlistService;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.WaitlistStatus;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fila")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
@Tag(name = "Fila de espera", description = "Entrada, ordenação, prioridade, reserva e recusa (RF06, RN29–RN34)")
@SecurityRequirement(name = "bearerAuth")
public class WaitlistController {

    private final WaitlistService waitlistService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inclui o beneficiário na fila do tipo (409 se já estiver na fila desse tipo)")
    public WaitlistEntryResponse enqueue(@Valid @RequestBody WaitlistEnqueueRequest request) {
        return waitlistService.enqueue(request);
    }

    @GetMapping
    @Operation(summary = "Lista a fila na ordem efetiva (sem status: AGUARDANDO e RESERVADO)")
    public List<WaitlistEntryResponse> findAll(@RequestParam(name = "tipo", required = false) EquipmentType type,
                                               @RequestParam(name = "status", required = false) WaitlistStatus status) {
        return waitlistService.findAll(type, status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha uma entrada da fila")
    public WaitlistEntryResponse findById(@PathVariable Long id) {
        return waitlistService.findById(id);
    }

    @PatchMapping("/{id}/prioridade")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Concede prioridade justificada (RN31)")
    public WaitlistEntryResponse setPriority(@PathVariable Long id,
                                             @Valid @RequestBody WaitlistPriorityRequest request) {
        return waitlistService.setPriority(id, request);
    }

    @DeleteMapping("/{id}/prioridade")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove a prioridade")
    public WaitlistEntryResponse clearPriority(@PathVariable Long id) {
        return waitlistService.clearPriority(id);
    }

    @PostMapping("/{id}/recusa")
    @Operation(summary = "Registra a recusa da reserva pelo beneficiário (RN33)")
    public WaitlistEntryResponse decline(@PathVariable Long id) {
        return waitlistService.decline(id);
    }

    @PatchMapping("/{id}/remover")
    @Operation(summary = "Remove o beneficiário da fila com motivo")
    public WaitlistEntryResponse remove(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        return waitlistService.remove(id, request.reason());
    }
}
