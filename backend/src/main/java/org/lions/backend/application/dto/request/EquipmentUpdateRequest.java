package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.EquipmentType;

/** Atualização cadastral. Patrimônio é imutável e o status só muda pelas transições (RN12). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentUpdateRequest {
    private EquipmentType type;

    @Size(max = 50, message = "O tamanho deve ter no máximo 50 caracteres.")
    private String size;

    private EquipmentCondition condition;
    private String notes;
    private String photoUrl;
}
