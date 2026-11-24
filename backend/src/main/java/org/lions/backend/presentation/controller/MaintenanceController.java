package org.lions.backend.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.request.MaintenanceCloseRequest;
import org.lions.backend.application.dto.request.MaintenanceCostRequest;
import org.lions.backend.application.dto.request.MaintenanceCreateRequest;
import org.lions.backend.application.dto.request.MaintenanceUpdateRequest;
import org.lions.backend.application.dto.response.MaintenanceResponse;
import org.lions.backend.application.dto.response.PreventiveMaintenanceDue;
import org.lions.backend.application.service.maintenance.MaintenanceService;
import org.lions.backend.application.service.maintenance.PreventiveMaintenanceService;
import org.lions.backend.domain.enums.MaintenanceStatus;
import org.lions.backend.domain.enums.MaintenanceType;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/v1/manutencoes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
@Tag(name = "Manutenções", description = "Manutenção corretiva e preventiva, custos e parecer (RF08, RN35–RN41)")
@SecurityRequirement(name = "bearerAuth")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;
    private final PreventiveMaintenanceService preventiveMaintenanceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abre manutenção para um equipamento")
    public MaintenanceResponse open(@Valid @RequestBody MaintenanceCreateRequest request) {
        return maintenanceService.open(request);
    }

    @GetMapping
    @Operation(summary = "Lista manutenções com filtros por status, equipamento e tipo")
    public List<MaintenanceResponse> findAll(@RequestParam(name = "status", required = false) MaintenanceStatus status,
                                             @RequestParam(name = "equipamentoId", required = false) Long equipmentId,
                                             @RequestParam(name = "tipo", required = false) MaintenanceType type) {
        return maintenanceService.findAll(status, equipmentId, type);
    }

    @GetMapping("/preventivas-pendentes")
    @Operation(summary = "Equipamentos com manutenção preventiva vencida (RN39)")
    public List<PreventiveMaintenanceDue> findPreventiveDue() {
        return preventiveMaintenanceService.findDue();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha uma manutenção")
    public MaintenanceResponse findById(@PathVariable Long id) {
        return maintenanceService.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza descrição do defeito e responsável técnico (só em andamento)")
    public MaintenanceResponse update(@PathVariable Long id, @Valid @RequestBody MaintenanceUpdateRequest request) {
        return maintenanceService.update(id, request);
    }

    @PostMapping("/{id}/custos")
    @Operation(summary = "Lança custo de peça ou serviço (ajuste de custo só Admin — RN41)")
    public MaintenanceResponse addCost(@PathVariable Long id, @Valid @RequestBody MaintenanceCostRequest request) {
        return maintenanceService.addCost(id, request);
    }

    @PostMapping("/{id}/encerrar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Encerra com parecer APTO ou INSERVIVEL — somente Admin (UC23, RN37)")
    public MaintenanceResponse close(@PathVariable Long id, @Valid @RequestBody MaintenanceCloseRequest request) {
        return maintenanceService.close(id, request);
    }
}
