package org.lions.backend.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.lions.backend.application.service.report.ReportService;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.ReportFormat;
import org.lions.backend.domain.enums.ReportType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/relatorios")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
@Tag(name = "Relatórios", description = "Exportação CSV/PDF de inventário, empréstimos, assistidos e manutenções (RF11)")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/{tipo}")
    @Operation(summary = "Gera o relatório (INVENTARIO, EMPRESTIMOS, ASSISTIDOS, MANUTENCOES) em CSV ou PDF",
            description = "Relatórios transacionais usam o período informado ou os últimos 30 dias (RN45). Perfil "
                    + "Consulta recebe dados pessoais mascarados (RN43). Toda exportação é auditada (RN44).")
    public ResponseEntity<byte[]> report(
            @PathVariable("tipo") ReportType type,
            @RequestParam(name = "formato", required = false, defaultValue = "CSV") ReportFormat format,
            @RequestParam(name = "inicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(name = "fim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(name = "tipoEquipamento", required = false) EquipmentType equipmentType) {
        ReportService.ReportFile file = reportService.generate(type, format, start, end, equipmentType);
        return FileResponses.attachment(file.content(), file.contentType(), file.fileName());
    }
}
