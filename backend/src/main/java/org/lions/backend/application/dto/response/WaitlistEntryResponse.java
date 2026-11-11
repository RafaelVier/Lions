package org.lions.backend.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lions.backend.domain.entity.Beneficiary;
import org.lions.backend.domain.entity.WaitlistEntry;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.PriorityReason;
import org.lions.backend.domain.enums.WaitlistStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitlistEntryResponse {
    private Long id;
    private Long beneficiaryId;
    private String beneficiaryName;
    private EquipmentType equipmentType;
    private String equipmentTypeDescription;
    private Long orderId;
    private WaitlistStatus status;
    private String statusDescription;
    /** Posição efetiva na fila do tipo (1 = próximo); preenchida pelo serviço quando AGUARDANDO. */
    private Integer position;
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

    public static WaitlistEntryResponse from(WaitlistEntry entry) {
        Beneficiary beneficiary = entry.getBeneficiary();
        return WaitlistEntryResponse.builder()
                .id(entry.getId())
                .beneficiaryId(beneficiary != null ? beneficiary.getId() : null)
                .beneficiaryName(beneficiary != null ? beneficiary.getFullName() : null)
                .equipmentType(entry.getEquipmentType())
                .equipmentTypeDescription(entry.getEquipmentType() != null ? entry.getEquipmentType().getDescription() : null)
                .orderId(entry.getOrderId())
                .status(entry.getStatus())
                .statusDescription(entry.getStatus() != null ? entry.getStatus().getDescription() : null)
                .queuedAt(entry.getQueuedAt())
                .originalRequestedAt(entry.getOriginalRequestedAt())
                .priority(entry.isPriority())
                .priorityReason(entry.getPriorityReason())
                .priorityJustification(entry.getPriorityJustification())
                .priorityAuthorizedBy(entry.getPriorityAuthorizedBy())
                .reservedEquipmentId(entry.getReservedEquipmentId())
                .reservedAt(entry.getReservedAt())
                .reservationExpiresAt(entry.getReservationExpiresAt())
                .consecutiveExpirations(entry.getConsecutiveExpirations())
                .removedReason(entry.getRemovedReason())
                .closedAt(entry.getClosedAt())
                .createdBy(entry.getCreatedBy())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }
}
