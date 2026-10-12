package org.lions.backend.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
    private boolean isForSelf;
    private String requesterName;
    private String relationship;
    private String beneficiaryName;
    private DocumentType documentType;
    private String maskedDocument; // LGPD: Exibição mascarada
    private String whatsapp;
    private String addressSummary;
    private String cep;
    private String street;
    private String number;
    private String neighborhood;
    private String city;
    private String state;
    private EquipmentType equipmentType;
    private String equipmentTypeDescription;
    private Integer quantity;
    private String needReason;
    private OrderStatus status;
    private String statusDescription;
    private String rejectionReason;
    private boolean assistedRegistration;
    private LocalDateTime createdAt;
}
