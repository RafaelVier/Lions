package org.lions.backend.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.EquipmentType;

import java.util.List;

/**
 * Atualização cadastral. Patrimônio é imutável e o status só muda pelas transições (RN12). Campos nulos não são
 * alterados; {@code specifications} informado substitui a lista inteira (lista vazia remove todas).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentUpdateRequest {
    private EquipmentType type;

    @Size(max = 50, message = "O tamanho deve ter no máximo 50 caracteres.")
    private String size;

    @Min(value = 1900, message = "Ano inválido.")
    @Max(value = 2100, message = "Ano inválido.")
    private Integer manufactureYear;

    @Valid
    private List<EquipmentSpecificationRequest> specifications;

    private EquipmentCondition condition;
    private String notes;
    private String photoUrl;
}
