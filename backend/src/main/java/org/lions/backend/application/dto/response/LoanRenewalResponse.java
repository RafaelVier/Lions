package org.lions.backend.application.dto.response;

import org.lions.backend.domain.entity.LoanRenewal;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Renovação registrada (RN23/RN25) — imutável. */
public record LoanRenewalResponse(
        Long id,
        Long loanId,
        LocalDate previousDueDate,
        LocalDate newDueDate,
        String renewedBy,
        LocalDateTime renewedAt) {

    public static LoanRenewalResponse from(LoanRenewal renewal) {
        return new LoanRenewalResponse(renewal.getId(), renewal.getLoanId(), renewal.getPreviousDueDate(),
                renewal.getNewDueDate(), renewal.getRenewedBy(), renewal.getRenewedAt());
    }
}
