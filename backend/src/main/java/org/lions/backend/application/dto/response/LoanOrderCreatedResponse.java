package org.lions.backend.application.dto.response;

import org.lions.backend.domain.enums.OrderStatus;

import java.time.LocalDateTime;

/** Protocolo da solicitação criada — sem dados pessoais (resposta segura para a rota pública). */
public record LoanOrderCreatedResponse(
        Long id,
        OrderStatus status,
        String statusDescription,
        LocalDateTime createdAt,
        String message) {
}
