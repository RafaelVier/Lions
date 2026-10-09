package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApprovalRequest {

    @NotNull(message = "O ID do equipamento a ser vinculado é obrigatório.")
    private Long equipmentId;

    private String deliveryNotes;
    private String deliveryPhotoUrl;
}
