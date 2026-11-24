package org.lions.backend.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.request.LoanOrderApprovalRequest;
import org.lions.backend.application.dto.request.LoanOrderCreateRequest;
import org.lions.backend.application.dto.request.ReasonRequest;
import org.lions.backend.application.dto.response.LoanOrderCreatedResponse;
import org.lions.backend.application.dto.response.LoanOrderDecisionResponse;
import org.lions.backend.application.dto.response.LoanOrderResponse;
import org.lions.backend.application.service.order.LoanOrderService;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.OrderStatus;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
@Tag(name = "Solicitações", description = "Pedido público de empréstimo, análise, aprovação e fila (RF09)")
@SecurityRequirement(name = "bearerAuth")
public class LoanOrderController {

    private final LoanOrderService loanOrderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("permitAll()")
    @Operation(summary = "Registra um pedido de empréstimo (público, sem login) e devolve o protocolo")
    public LoanOrderCreatedResponse create(@Valid @RequestBody LoanOrderCreateRequest request) {
        return loanOrderService.create(request);
    }

    @GetMapping
    @Operation(summary = "Lista pedidos com filtros por status, tipo de equipamento e beneficiário")
    public List<LoanOrderResponse> findAll(@RequestParam(name = "status", required = false) OrderStatus status,
                                           @RequestParam(name = "tipo", required = false) EquipmentType type,
                                           @RequestParam(name = "beneficiarioId", required = false) Long beneficiaryId) {
        return loanOrderService.findAll(status, type, beneficiaryId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um pedido")
    public LoanOrderResponse findById(@PathVariable Long id) {
        return loanOrderService.findById(id);
    }

    @PostMapping("/{id}/aprovar")
    @Operation(summary = "Aprova o pedido registrando o empréstimo com o equipamento escolhido")
    public LoanOrderDecisionResponse approve(@PathVariable Long id,
                                             @Valid @RequestBody LoanOrderApprovalRequest request) {
        return loanOrderService.approve(id, request);
    }

    @PostMapping("/{id}/rejeitar")
    @Operation(summary = "Rejeita o pedido com motivo")
    public LoanOrderResponse reject(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        return loanOrderService.reject(id, request.reason());
    }

    @PostMapping("/{id}/fila")
    @Operation(summary = "Envia o pedido para a fila de espera (sem estoque do tipo)")
    public LoanOrderDecisionResponse sendToWaitlist(@PathVariable Long id) {
        return loanOrderService.sendToWaitlist(id);
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancela o pedido com motivo")
    public LoanOrderResponse cancel(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        return loanOrderService.cancel(id, request.reason());
    }
}
