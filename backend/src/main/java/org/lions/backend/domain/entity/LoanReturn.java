package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.LoanStatus;
import org.lions.backend.domain.enums.ReturnCondition;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidade de Domínio (DDD): Devolução do Equipamento
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanReturn {

    private Long id;
    private Loan loan;
    private LocalDate returnDate;
    private ReturnCondition returnCondition;
    private String damageDescription;
    private String returnPhotoUrl;
    private boolean hasDamage;
    private boolean responsibilityAssigned;
    private LocalDateTime createdAt;

    public void processReturn() {
        if (returnDate == null) {
            returnDate = LocalDate.now();
        }
        if (loan != null) {
            loan.setStatus(LoanStatus.DEVOLVIDO);
            Equipment equipment = loan.getEquipment();
            if (equipment != null) {
                if (ReturnCondition.DANIFICADO.equals(returnCondition)) {
                    this.hasDamage = true;
                    equipment.markAsReturned(EquipmentCondition.EM_MANUTENCAO);
                } else if (ReturnCondition.PRECISA_MANUTENCAO.equals(returnCondition)) {
                    equipment.markAsReturned(EquipmentCondition.EM_MANUTENCAO);
                } else {
                    equipment.markAsReturned(EquipmentCondition.USADO);
                }
            }
        }
    }
}
