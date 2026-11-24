package org.lions.backend.application.dto.response;

import org.lions.backend.domain.entity.LoanReturn;
import org.lions.backend.domain.enums.ReturnCondition;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Devolução registrada (RN24/RN25) — imutável. */
public record LoanReturnResponse(
        Long id,
        Long loanId,
        LocalDate returnDate,
        ReturnCondition returnCondition,
        String returnConditionDescription,
        String damageDescription,
        String returnPhotoUrl,
        boolean hasDamage,
        String receivedBy,
        LocalDateTime createdAt) {

    public static LoanReturnResponse from(LoanReturn loanReturn) {
        return new LoanReturnResponse(
                loanReturn.getId(),
                loanReturn.getLoanId(),
                loanReturn.getReturnDate(),
                loanReturn.getReturnCondition(),
                loanReturn.getReturnCondition() != null ? loanReturn.getReturnCondition().getDescription() : null,
                loanReturn.getDamageDescription(),
                loanReturn.getReturnPhotoUrl(),
                loanReturn.isHasDamage(),
                loanReturn.getReceivedBy(),
                loanReturn.getCreatedAt());
    }
}
