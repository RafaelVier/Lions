package org.lions.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.LoanStatus;
import org.lions.backend.domain.enums.SignatureType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_loans", indexes = {
        @Index(name = "idx_loan_status", columnList = "status"),
        @Index(name = "idx_loan_beneficiary", columnList = "beneficiary_id"),
        @Index(name = "idx_loan_equipment", columnList = "equipment_id"),
        @Index(name = "idx_loan_expected_return", columnList = "expected_return_date")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beneficiary_id", nullable = false)
    private BeneficiaryJpaEntity beneficiary;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private EquipmentJpaEntity equipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private LoanOrderJpaEntity order;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private LoanStatus status;

    @Column(nullable = false)
    private LocalDate loanDate;

    @Column(nullable = false)
    private LocalDate initialExpectedReturnDate;

    @Column(name = "expected_return_date", nullable = false)
    private LocalDate expectedReturnDate;

    @Column(nullable = false)
    private int renewalCount;

    private LocalDate lastRenewalDate;

    @Column(nullable = false)
    private boolean retroactive;

    @Column(length = 500)
    private String retroactiveJustification;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EquipmentCondition initialCondition;

    @Column(length = 500)
    private String deliveryNotes;

    @Column(length = 500)
    private String deliveryPhotoUrl;

    @Column(length = 300)
    private String termPdfPath;

    @Column(length = 64)
    private String termHashSha256;

    private LocalDateTime termGeneratedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private SignatureType signatureType;

    private LocalDateTime signedAt;

    @Column(length = 150)
    private String signedByName;

    @Column(length = 300)
    private String signedDocumentPath;

    @Column(length = 50)
    private String signatureRegisteredBy;

    @Column(length = 50)
    private String createdBy;

    private LocalDateTime returnedAt;

    @Column(length = 500)
    private String cancelReason;

    @Column(length = 50)
    private String canceledBy;

    private LocalDateTime canceledAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
