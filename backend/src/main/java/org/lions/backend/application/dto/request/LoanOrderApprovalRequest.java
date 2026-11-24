package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Aprovação da solicitação: registra o empréstimo do equipamento escolhido (→ {@code LoanService.registerLoan}).
 * {@code loanDate} nula = hoje; data passada = retroativo (somente Admin, com justificativa — RN22).
 */
public record LoanOrderApprovalRequest(
        @NotNull(message = "O equipamento é obrigatório.")
        Long equipmentId,
        LocalDate loanDate,
        @Size(max = 1000, message = "A justificativa deve ter no máximo 1000 caracteres.")
        String retroactiveJustification,
        @Size(max = 2000, message = "As observações de entrega devem ter no máximo 2000 caracteres.")
        String deliveryNotes,
        @Size(max = 500, message = "A URL da foto deve ter no máximo 500 caracteres.")
        String deliveryPhotoUrl) {
}
