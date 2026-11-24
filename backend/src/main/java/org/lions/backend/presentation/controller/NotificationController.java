package org.lions.backend.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.response.DailySummaryResponse;
import org.lions.backend.application.dto.response.NotificationResponse;
import org.lions.backend.application.service.notification.DailySummaryService;
import org.lions.backend.application.service.notification.NotificationService;
import org.lions.backend.domain.enums.NotificationStatus;
import org.lions.backend.domain.enums.NotificationType;
import org.lions.backend.domain.enums.RecipientType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notificacoes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
@Tag(name = "Notificações", description = "Notificações enviadas e tarefas de contato manual por WhatsApp (RF12, RN46–RN49)")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;
    private final DailySummaryService dailySummaryService;

    @GetMapping
    @Operation(summary = "Lista notificações com filtros por status, tipo, destinatário e beneficiário")
    public List<NotificationResponse> findAll(
            @RequestParam(name = "status", required = false) NotificationStatus status,
            @RequestParam(name = "tipo", required = false) NotificationType type,
            @RequestParam(name = "destinatario", required = false) RecipientType recipientType,
            @RequestParam(name = "beneficiarioId", required = false) Long beneficiaryId) {
        return notificationService.findAll(status, type, recipientType, beneficiaryId);
    }

    @GetMapping("/contatos-manuais")
    @Operation(summary = "Tarefas de contato manual pendentes, com link wa.me pronto (botão \"Lembrar no WhatsApp\")")
    public List<NotificationResponse> findManualContacts() {
        return notificationService.findManualContacts();
    }

    @GetMapping("/resumo-diario")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resumo diário do Administrador: atrasos, reservas expiradas (24 h), manutenções em aberto e "
            + "preventivas vencidas (RN49)")
    public DailySummaryResponse dailySummary() {
        return dailySummaryService.build();
    }

    @PatchMapping("/{id}/concluir")
    @Operation(summary = "Marca o contato manual como realizado")
    public NotificationResponse complete(@PathVariable Long id) {
        return notificationService.complete(id);
    }
}
