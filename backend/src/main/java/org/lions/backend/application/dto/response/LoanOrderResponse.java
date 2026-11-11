package org.lions.backend.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lions.backend.application.support.LgpdMasker;
import org.lions.backend.domain.entity.Beneficiary;
import org.lions.backend.domain.entity.LoanOrder;
import org.lions.backend.domain.enums.DocumentType;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.OrderStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanOrderResponse {
    private Long id;
    private Long beneficiaryId;
    private String beneficiaryName;
    private DocumentType documentType;
    private String maskedDocument;
    private String phone;
    private String city;
    private String state;
    private boolean forSelf;
    private String requesterName;
    private String requesterRelationship;
    private EquipmentType equipmentType;
    private String equipmentTypeDescription;
    private String needReason;
    private OrderStatus status;
    private String statusDescription;
    private String rejectionReason;
    private boolean assistedRegistration;
    private String decidedBy;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;

    public static LoanOrderResponse from(LoanOrder order) {
        Beneficiary b = order.getBeneficiary();
        return LoanOrderResponse.builder()
                .id(order.getId())
                .beneficiaryId(b != null ? b.getId() : null)
                .beneficiaryName(b != null ? b.getFullName() : null)
                .documentType(b != null ? b.getDocumentType() : null)
                .maskedDocument(b != null ? LgpdMasker.maskDocument(b.getDocumentNumber()) : null)
                .phone(b != null ? b.getPhone() : null)
                .city(b != null ? b.getCity() : null)
                .state(b != null ? b.getState() : null)
                .forSelf(order.isForSelf())
                .requesterName(order.getRequesterName())
                .requesterRelationship(order.getRequesterRelationship())
                .equipmentType(order.getEquipmentType())
                .equipmentTypeDescription(order.getEquipmentType() != null ? order.getEquipmentType().getDescription() : null)
                .needReason(order.getNeedReason())
                .status(order.getStatus())
                .statusDescription(order.getStatus() != null ? order.getStatus().getDescription() : null)
                .rejectionReason(order.getRejectionReason())
                .assistedRegistration(order.isAssistedRegistration())
                .decidedBy(order.getDecidedBy())
                .decidedAt(order.getDecidedAt())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
