package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Registro de empréstimo (RN17–RN22). {@code loanDate} nula = hoje; data passada = retroativo (só ADMIN, RN22). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanCreateRequest {

    @NotNull(message = "O beneficiário é obrigatório.")
    private Long beneficiaryId;

    @NotNull(message = "O equipamento é obrigatório.")
    private Long equipmentId;

    private Long orderId;

    private LocalDate loanDate;

    @Size(max = 1000, message = "A justificativa deve ter no máximo 1000 caracteres.")
    private String retroactiveJustification;

    @Size(max = 2000, message = "As observações de entrega devem ter no máximo 2000 caracteres.")
    private String deliveryNotes;

    @Size(max = 500, message = "A URL da foto deve ter no máximo 500 caracteres.")
    private String deliveryPhotoUrl;
}
