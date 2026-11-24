package org.lions.backend.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.response.AuditLogResponse;
import org.lions.backend.application.service.audit.AuditService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Auditoria", description = "Consulta da trilha de auditoria (RN58)")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "Consulta a trilha por entidade, registro e período (datas inclusivas, yyyy-MM-dd)")
    public List<AuditLogResponse> search(
            @RequestParam(name = "entidade", required = false) String entityName,
            @RequestParam(name = "entidadeId", required = false) String entityId,
            @RequestParam(name = "inicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate start,
            @RequestParam(name = "fim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate end) {
        return auditService.searchResponses(entityName, entityId,
                start != null ? start.atStartOfDay() : null,
                end != null ? end.atTime(LocalTime.MAX) : null);
    }
}
