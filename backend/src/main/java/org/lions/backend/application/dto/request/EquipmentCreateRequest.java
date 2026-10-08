package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.EquipmentType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentCreateRequest {

    @NotBlank(message = "O número de patrimônio é obrigatório.")
    private String patrimonyNumber; // Ex: PT-0142

    @NotNull(message = "O tipo de equipamento é obrigatório.")
    private EquipmentType type;

    @NotNull(message = "A condição do equipamento é obrigatória.")
    private EquipmentCondition condition;

    private EquipmentStatus status; // Padrão: DISPONIVEL
    private String notes;
    private String photoUrl;
}
