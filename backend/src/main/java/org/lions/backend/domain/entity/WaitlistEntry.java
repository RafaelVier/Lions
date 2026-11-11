package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.PriorityReason;
import org.lions.backend.domain.enums.WaitlistStatus;
import org.lions.backend.domain.exception.BusinessException;

import java.time.LocalDateTime;

/** Posição na fila de espera por tipo de equipamento (RN29–RN34). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitlistEntry {

    public static final String REMOVED_BY_EXPIRATIONS = "Removido após expirações/recusas consecutivas de reserva.";

    private Long id;
    private Beneficiary beneficiary;
    private EquipmentType equipmentType;
    private Long orderId;
    @Builder.Default
    private WaitlistStatus status = WaitlistStatus.AGUARDANDO;
    /** Base da ordenação FIFO; reinicia quando a posição volta ao fim da fila (RN33). */
    private LocalDateTime queuedAt;
    private LocalDateTime originalRequestedAt;
    private boolean priority;
    private PriorityReason priorityReason;
    private String priorityJustification;
    private String priorityAuthorizedBy;
    private Long reservedEquipmentId;
    private LocalDateTime reservedAt;
    private LocalDateTime reservationExpiresAt;
    private int consecutiveExpirations;
    private String removedReason;
    private LocalDateTime closedAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isActive() {
        return status == WaitlistStatus.AGUARDANDO || status == WaitlistStatus.RESERVADO;
    }

    /** RN32 — AGUARDANDO → RESERVADO por {@code hours} horas. */
    public void reserve(Long equipmentId, LocalDateTime now, int hours) {
        if (status != WaitlistStatus.AGUARDANDO) {
            throw new BusinessException("Só é possível reservar para uma posição aguardando na fila.");
        }
        if (equipmentId == null) {
            throw new BusinessException("Equipamento da reserva é obrigatório.");
        }
        if (hours <= 0) {
            throw new BusinessException("Prazo da reserva deve ser positivo.");
        }
        this.status = WaitlistStatus.RESERVADO;
        this.reservedEquipmentId = equipmentId;
        this.reservedAt = now;
        this.reservationExpiresAt = now.plusHours(hours);
    }

    public boolean isReservationExpired(LocalDateTime now) {
        return status == WaitlistStatus.RESERVADO && reservationExpiresAt != null && now.isAfter(reservationExpiresAt);
    }

    /**
     * RN33 — reserva expirada ou recusada: volta ao fim da fila ({@code queuedAt = now}) ou, ao atingir
     * {@code maxConsecutive} expirações/recusas consecutivas, é removido. Retorna {@code true} se foi removido.
     */
    public boolean expireOrDecline(LocalDateTime now, int maxConsecutive) {
        if (status != WaitlistStatus.RESERVADO) {
            throw new BusinessException("Posição na fila não possui reserva ativa.");
        }
        clearReservation();
        this.consecutiveExpirations++;
        if (consecutiveExpirations >= maxConsecutive) {
            this.status = WaitlistStatus.REMOVIDO;
            this.removedReason = REMOVED_BY_EXPIRATIONS;
            this.closedAt = now;
            return true;
        }
        this.status = WaitlistStatus.AGUARDANDO;
        this.queuedAt = now;
        return false;
    }

    /** Beneficiário atendido (empréstimo registrado). */
    public void serve(LocalDateTime now) {
        if (!isActive()) {
            throw new BusinessException("Posição na fila não está ativa.");
        }
        this.status = WaitlistStatus.ATENDIDO;
        this.consecutiveExpirations = 0;
        this.closedAt = now;
    }

    /** RN34 — remoção (inativação/bloqueio do beneficiário, desistência etc.). */
    public void remove(String reason, LocalDateTime now) {
        if (!isActive()) {
            throw new BusinessException("Posição na fila não está ativa.");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Motivo da remoção da fila é obrigatório.");
        }
        clearReservation();
        this.status = WaitlistStatus.REMOVIDO;
        this.removedReason = reason;
        this.closedAt = now;
    }

    /** RN30 — prioridade com motivo e justificativa (autorização de Admin verificada no serviço). */
    public void setPriority(PriorityReason reason, String justification, String by) {
        if (!isActive()) {
            throw new BusinessException("Só posições ativas na fila podem receber prioridade.");
        }
        if (reason == null) {
            throw new BusinessException("Motivo da prioridade é obrigatório.");
        }
        if (justification == null || justification.isBlank()) {
            throw new BusinessException("Justificativa da prioridade é obrigatória.");
        }
        this.priority = true;
        this.priorityReason = reason;
        this.priorityJustification = justification;
        this.priorityAuthorizedBy = by;
    }

    public void clearPriority() {
        this.priority = false;
        this.priorityReason = null;
        this.priorityJustification = null;
        this.priorityAuthorizedBy = null;
    }

    private void clearReservation() {
        this.reservedEquipmentId = null;
        this.reservedAt = null;
        this.reservationExpiresAt = null;
    }
}
