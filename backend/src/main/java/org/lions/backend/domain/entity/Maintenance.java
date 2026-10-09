package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.MaintenanceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidade de Domínio (DDD): Manutenção de Equipamento
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Maintenance {

    private Long id;
    private Equipment equipment;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private BigDecimal cost;
    private MaintenanceStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void start() {
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        this.status = MaintenanceStatus.EM_ANDAMENTO;
        if (equipment != null) {
            equipment.sendToMaintenance();
        }
    }

    public void complete(EquipmentCondition conditionAfterFix) {
        this.endDate = LocalDate.now();
        this.status = MaintenanceStatus.CONCLUIDA;
        if (equipment != null) {
            equipment.returnFromMaintenance(conditionAfterFix);
        }
    }
}
