package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.lions.backend.domain.enums.ReturnCondition;
import org.lions.backend.domain.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Registro imutável da devolução de um empréstimo (RN24/RN25). */
@Getter
@Builder
@AllArgsConstructor
public class LoanReturn {

    private final Long id;
    private final Long loanId;
    private final LocalDate returnDate;
    private final ReturnCondition returnCondition;
    private final String damageDescription;
    private final String returnPhotoUrl;
    private final boolean hasDamage;
    private final String receivedBy;
    private final LocalDateTime createdAt;

    /** Cria a devolução validando a RN24 (condição obrigatória; avaria exige descrição). */
    public static LoanReturn register(Long loanId, LocalDate returnDate, ReturnCondition condition,
                                      String damageDescription, String returnPhotoUrl, String receivedBy) {
        if (loanId == null) {
            throw new BusinessException("Empréstimo da devolução é obrigatório.");
        }
        if (returnDate == null) {
            throw new BusinessException("Data da devolução é obrigatória.");
        }
        if (condition == null) {
            throw new BusinessException("Condição do equipamento na devolução é obrigatória.");
        }
        boolean damaged = condition == ReturnCondition.DANIFICADO;
        if (damaged && (damageDescription == null || damageDescription.isBlank())) {
            throw new BusinessException("Devolução com dano exige a descrição da avaria.");
        }
        return LoanReturn.builder()
                .loanId(loanId)
                .returnDate(returnDate)
                .returnCondition(condition)
                .damageDescription(damageDescription)
                .returnPhotoUrl(returnPhotoUrl)
                .hasDamage(damaged)
                .receivedBy(receivedBy)
                .build();
    }

    public boolean needsMaintenance() {
        return returnCondition != null && returnCondition != ReturnCondition.BOM_ESTADO;
    }
}
