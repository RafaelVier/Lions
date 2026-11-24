package org.lions.backend.application.dto.response;

import java.util.List;

/**
 * Histórico consolidado do beneficiário (RN42): empréstimos (com renovações e devolução), solicitações, fila e
 * ocorrências — inclusive de equipamentos baixados — e a situação atual (RN20).
 */
public record BeneficiaryHistoryResponse(
        BeneficiaryResponse beneficiary,
        BeneficiaryRestriction restriction,
        List<LoanResponse> loans,
        List<LoanOrderResponse> orders,
        List<WaitlistEntryResponse> waitlist,
        List<OccurrenceResponse> occurrences) {
}
