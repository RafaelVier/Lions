package org.lions.backend.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.EquipmentType;

import java.util.List;

/** Cadastro de equipamento. Patrimônio é gerado pelo sistema ({@code PT-%04d}) e o status inicial é DISPONIVEL. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentCreateRequest {

    @NotNull(message = "O tipo de equipamento é obrigatório.")
    private EquipmentType type;

    @Size(max = 50, message = "O tamanho deve ter no máximo 50 caracteres.")
    private String size;

    @Min(value = 1900, message = "Ano inválido.")
    @Max(value = 2100, message = "Ano inválido.")
    private Integer manufactureYear;

    @Valid
    private List<EquipmentSpecificationRequest> specifications;

    @NotNull(message = "A condição do equipamento é obrigatória.")
    private EquipmentCondition condition;

    private String notes;
    private String photoUrl;
}
