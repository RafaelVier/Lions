package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.OrderStatus;
import org.lions.backend.domain.exception.BusinessException;

import java.time.LocalDateTime;

/**
 * Entidade de Domínio (DDD): Pedido de Empréstimo
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanOrder {

    private Long id;
    private Requester requester;
    private EquipmentType equipmentType;
    private Integer quantity; // Geralmente 1
    private OrderStatus status;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void approve() {
        if (!OrderStatus.PENDENTE_ANALISE.equals(this.status) && !OrderStatus.PENDENTE_ESTOQUE.equals(this.status)) {
            throw new BusinessException("Apenas pedidos pendentes podem ser aprovados.");
        }
        this.status = OrderStatus.APROVADO;
        this.updatedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Informe o motivo da rejeição do pedido.");
        }
        this.status = OrderStatus.REJEITADO;
        this.rejectionReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public void markPendingStock() {
        this.status = OrderStatus.PENDENTE_ESTOQUE;
        this.updatedAt = LocalDateTime.now();
    }
}
