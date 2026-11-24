package org.lions.backend.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lions.backend.domain.entity.Beneficiary;
import org.lions.backend.domain.entity.Equipment;
import org.lions.backend.domain.entity.Loan;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.LoanStatus;
import org.lions.backend.domain.enums.SignatureType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanResponse {
    private Long id;
    private Long beneficiaryId;
    private String beneficiaryName;
    private Long equipmentId;
    private String patrimonyNumber;
    private EquipmentType equipmentType;
    private String equipmentTypeDescription;
    private Long orderId;
    private LoanStatus status;
    private String statusDescription;
    private LocalDate loanDate;
    private LocalDate initialExpectedReturnDate;
    private LocalDate expectedReturnDate;
    private int renewalCount;
    private LocalDate lastRenewalDate;
    private boolean retroactive;
    private String retroactiveJustification;
    private EquipmentCondition initialCondition;
    private String deliveryNotes;
    private String deliveryPhotoUrl;
    private boolean termAvailable;
    private String termHashSha256;
    private LocalDateTime termGeneratedAt;
    private SignatureType signatureType;
    private LocalDateTime signedAt;
    private String signedByName;
    private String signatureRegisteredBy;
    private String createdBy;
    private LocalDateTime returnedAt;
    private String cancelReason;
    private String canceledBy;
    private LocalDateTime canceledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean overdue;
    private long daysOverdue;
    private boolean dueSoon;

    // ---------- acréscimos (Frente D) ----------
    private String beneficiaryPhone;
    /** Quem deve assinar o termo (responsável legal quando exigido — RN06). */
    private String signatoryName;
    private boolean requiresLegalGuardian;
    /** Há termo assinado digitalizado (assinatura física) disponível para download. */
    private boolean signedDocumentAvailable;
    /** Link wa.me com lembrete pronto (preenchido em listas de vencimento/atraso). */
    private String whatsappLink;
    /** Detalhe: pode renovar agora (RN23) e, se não, por quê. */
    private Boolean canRenew;
    private List<String> renewalBlockers;
    /** Detalhe: renovações, devolução e ajustes (RN41). */
    private List<LoanRenewalResponse> renewals;
    private LoanReturnResponse returnInfo;
    private List<AdjustmentResponse> adjustments;

    /**
     * @param today       data de referência (do {@code Clock})
     * @param dueSoonDays janela de "vence em breve" (ex.: {@code loan.renewal-window-days})
     */
    public static LoanResponse from(Loan loan, LocalDate today, int dueSoonDays) {
        Beneficiary beneficiary = loan.getBeneficiary();
        Equipment equipment = loan.getEquipment();
        EquipmentType type = equipment != null ? equipment.getType() : null;
        return LoanResponse.builder()
                .id(loan.getId())
                .beneficiaryId(beneficiary != null ? beneficiary.getId() : null)
                .beneficiaryName(beneficiary != null ? beneficiary.getFullName() : null)
                .equipmentId(equipment != null ? equipment.getId() : null)
                .patrimonyNumber(equipment != null ? equipment.getPatrimonyNumber() : null)
                .equipmentType(type)
                .equipmentTypeDescription(type != null ? type.getDescription() : null)
                .orderId(loan.getOrder() != null ? loan.getOrder().getId() : null)
                .status(loan.getStatus())
                .statusDescription(loan.getStatus() != null ? loan.getStatus().getDescription() : null)
                .loanDate(loan.getLoanDate())
                .initialExpectedReturnDate(loan.getInitialExpectedReturnDate())
                .expectedReturnDate(loan.getExpectedReturnDate())
                .renewalCount(loan.getRenewalCount())
                .lastRenewalDate(loan.getLastRenewalDate())
                .retroactive(loan.isRetroactive())
                .retroactiveJustification(loan.getRetroactiveJustification())
                .initialCondition(loan.getInitialCondition())
                .deliveryNotes(loan.getDeliveryNotes())
                .deliveryPhotoUrl(loan.getDeliveryPhotoUrl())
                .termAvailable(loan.getTermPdfPath() != null)
                .termHashSha256(loan.getTermHashSha256())
                .termGeneratedAt(loan.getTermGeneratedAt())
                .signatureType(loan.getSignatureType())
                .signedAt(loan.getSignedAt())
                .signedByName(loan.getSignedByName())
                .signatureRegisteredBy(loan.getSignatureRegisteredBy())
                .createdBy(loan.getCreatedBy())
                .returnedAt(loan.getReturnedAt())
                .cancelReason(loan.getCancelReason())
                .canceledBy(loan.getCanceledBy())
                .canceledAt(loan.getCanceledAt())
                .createdAt(loan.getCreatedAt())
                .updatedAt(loan.getUpdatedAt())
                .overdue(loan.isOverdue(today))
                .daysOverdue(loan.daysOverdue(today))
                .dueSoon(loan.isDueSoon(today, dueSoonDays))
                .beneficiaryPhone(beneficiary != null ? beneficiary.getPhone() : null)
                .signatoryName(beneficiary != null && !beneficiary.isAnonymized()
                        ? beneficiary.signatoryName(today) : null)
                .requiresLegalGuardian(beneficiary != null && !beneficiary.isAnonymized()
                        && beneficiary.requiresLegalGuardian(today))
                .signedDocumentAvailable(loan.getSignedDocumentPath() != null)
                .build();
    }
}
