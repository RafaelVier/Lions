package org.lions.backend.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.response.DashboardResponse;
import org.lions.backend.application.service.dashboard.DashboardService;
import org.lions.backend.domain.enums.EquipmentType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
@Tag(name = "Dashboard", description = "Indicadores gerenciais em tempo real (RF15, RN54–RN56)")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Indicadores: ocupação, atrasos, fila atual, custos de manutenção e volume de empréstimos",
            description = "Período padrão: últimos 30 dias. Perfil Consulta recebe só números consolidados (RN56).")
    public DashboardResponse dashboard(
            @RequestParam(name = "inicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(name = "fim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(name = "tipo", required = false) EquipmentType type) {
        return dashboardService.build(start, end, type);
    }
}
