package org.lions.backend.application.dto.response;

/**
 * Resultado da decisão sobre a solicitação: na aprovação traz o empréstimo criado (próximo passo: assinatura do
 * termo); no envio à fila traz a posição criada.
 */
public record LoanOrderDecisionResponse(
        LoanOrderResponse order,
        LoanResponse loan,
        WaitlistEntryResponse waitlistEntry) {
}
