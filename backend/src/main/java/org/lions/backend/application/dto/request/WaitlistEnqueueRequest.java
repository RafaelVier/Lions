package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lions.backend.domain.enums.EquipmentType;

/** Inclusão na fila de espera (RN29, RN31, RN34). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitlistEnqueueRequest {

    @NotNull(message = "O beneficiário é obrigatório.")
    private Long beneficiaryId;

    @NotNull(message = "O tipo de equipamento é obrigatório.")
    private EquipmentType equipmentType;

    private Long orderId;
}
