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
import java.util.EnumSet;
import java.util.Set;

/**
 * Solicitação de empréstimo (pedido). Os dados de quem solicita em nome de terceiros ficam aqui;
 * os dados pessoais do assistido ficam em {@link Beneficiary}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanOrder {

    private Long id;
    private Beneficiary beneficiary;
    @Builder.Default
    private boolean forSelf = true;
    private String requesterName;
    private String requesterRelationship;
    private EquipmentType equipmentType;
    private String needReason;
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDENTE_ANALISE;
    /** Motivo da rejeição ou do cancelamento. */
    private String rejectionReason;
    private boolean assistedRegistration;
    private String decidedBy;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void validate() {
        if (beneficiary == null) {
            throw new BusinessException("Beneficiário da solicitação é obrigatório.");
        }
        if (equipmentType == null) {
            throw new BusinessException("Tipo de equipamento é obrigatório.");
        }
        if (!forSelf && (requesterName == null || requesterName.isBlank())) {
            throw new BusinessException("Nome do solicitante é obrigatório quando o pedido é feito para outra pessoa.");
        }
    }

    /** PENDENTE_ANALISE ou EM_FILA_ESPERA → APROVADO. */
    public void approve(String by, LocalDateTime now) {
        requireStatus(EnumSet.of(OrderStatus.PENDENTE_ANALISE, OrderStatus.EM_FILA_ESPERA), "aprovar");
        decide(OrderStatus.APROVADO, by, now);
    }

    /** PENDENTE_ANALISE → REJEITADO (motivo obrigatório). */
    public void reject(String reason, String by, LocalDateTime now) {
        requireStatus(EnumSet.of(OrderStatus.PENDENTE_ANALISE), "rejeitar");
        requireReason(reason, "rejeição");
        this.rejectionReason = reason;
        decide(OrderStatus.REJEITADO, by, now);
    }

    /** PENDENTE_ANALISE → EM_FILA_ESPERA. */
    public void sendToWaitlist(String by, LocalDateTime now) {
        requireStatus(EnumSet.of(OrderStatus.PENDENTE_ANALISE), "enviar para a fila de espera");
        decide(OrderStatus.EM_FILA_ESPERA, by, now);
    }

    /** PENDENTE_ANALISE ou EM_FILA_ESPERA (ex.: beneficiário removido da fila) → CANCELADO (motivo obrigatório). */
    public void cancel(String reason, String by, LocalDateTime now) {
        requireStatus(EnumSet.of(OrderStatus.PENDENTE_ANALISE, OrderStatus.EM_FILA_ESPERA), "cancelar");
        requireReason(reason, "cancelamento");
        this.rejectionReason = reason;
        decide(OrderStatus.CANCELADO, by, now);
    }

    public boolean isPending() {
        return status == OrderStatus.PENDENTE_ANALISE;
    }

    private void decide(OrderStatus target, String by, LocalDateTime now) {
        this.status = target;
        this.decidedBy = by;
        this.decidedAt = now;
    }

    private void requireStatus(Set<OrderStatus> allowed, String action) {
        if (!allowed.contains(status)) {
            throw new BusinessException("Não é possível " + action + " uma solicitação com status " + status + ".");
        }
    }

    private static void requireReason(String reason, String label) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Motivo do(a) " + label + " é obrigatório.");
        }
    }
}
