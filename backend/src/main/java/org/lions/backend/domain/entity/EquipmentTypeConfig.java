package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.exception.BusinessException;

import java.time.LocalDateTime;

/**
 * Cadastro do tipo de equipamento (MER: {@code tb_equipamento_tipo}). Os tipos são fixos (RN14 — enum
 * {@link EquipmentType}); a tabela guarda o rótulo exibido e o prazo de empréstimo do tipo (RN18).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentTypeConfig {

    private EquipmentType code;
    private String description;
    /** Prazo do empréstimo para o tipo, em dias; {@code null} = prazo padrão ({@code loan.default-days}). */
    private Integer loanDays;
    private String updatedBy;
    private LocalDateTime updatedAt;

    public static EquipmentTypeConfig defaultFor(EquipmentType type) {
        return EquipmentTypeConfig.builder().code(type).description(type.getDescription()).build();
    }

    /** RN18 — prazo do tipo ou, se não configurado, o prazo padrão. */
    public int effectiveLoanDays(int defaultDays) {
        return loanDays != null && loanDays > 0 ? loanDays : defaultDays;
    }

    public void update(String newDescription, Integer newLoanDays, String by, LocalDateTime now) {
        if (newDescription == null || newDescription.isBlank()) {
            throw new BusinessException("A descrição do tipo de equipamento é obrigatória.");
        }
        if (newLoanDays != null && newLoanDays <= 0) {
            throw new BusinessException("O prazo do empréstimo deve ser um número inteiro positivo de dias.");
        }
        this.description = newDescription.trim();
        this.loanDays = newLoanDays;
        this.updatedBy = by;
        this.updatedAt = now;
    }
}
