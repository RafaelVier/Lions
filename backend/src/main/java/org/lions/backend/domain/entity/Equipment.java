package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.exception.BusinessException;

import java.time.LocalDateTime;

/**
 * Entidade de Domínio (DDD): Equipamento Ortopédico
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Equipment {

    private Long id;
    private String patrimonyNumber; // Ex: "PT-0142"
    private EquipmentType type;
    private EquipmentCondition condition;
    private EquipmentStatus status;
    private String notes;
    private String photoUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void validate() {
        if (patrimonyNumber == null || patrimonyNumber.isBlank()) {
            throw new BusinessException("O número de patrimônio é obrigatório.");
        }
        if (type == null) {
            throw new BusinessException("O tipo de equipamento é obrigatório.");
        }
    }

    public boolean isAvailable() {
        return EquipmentStatus.DISPONIVEL.equals(this.status);
    }

    public void markAsLoaned() {
        if (!isAvailable()) {
            throw new BusinessException("O equipamento " + patrimonyNumber + " não está disponível para empréstimo.");
        }
        this.status = EquipmentStatus.EMPRESTADO;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsReturned(EquipmentCondition conditionAfterReturn) {
        this.condition = conditionAfterReturn;
        if (EquipmentCondition.EM_MANUTENCAO.equals(conditionAfterReturn)) {
            this.status = EquipmentStatus.EM_MANUTENCAO;
        } else {
            this.status = EquipmentStatus.DISPONIVEL;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void sendToMaintenance() {
        this.status = EquipmentStatus.EM_MANUTENCAO;
        this.condition = EquipmentCondition.EM_MANUTENCAO;
        this.updatedAt = LocalDateTime.now();
    }

    public void returnFromMaintenance(EquipmentCondition newCondition) {
        this.status = EquipmentStatus.DISPONIVEL;
        this.condition = newCondition;
        this.updatedAt = LocalDateTime.now();
    }

    public void retire() {
        this.status = EquipmentStatus.BAIXADO;
        this.condition = EquipmentCondition.BAIXADO;
        this.updatedAt = LocalDateTime.now();
    }
}
