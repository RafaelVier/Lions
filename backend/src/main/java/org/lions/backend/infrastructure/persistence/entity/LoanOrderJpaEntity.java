package org.lions.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.OrderStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_loan_orders", indexes = {
        @Index(name = "idx_loan_order_status", columnList = "status"),
        @Index(name = "idx_loan_order_beneficiary", columnList = "beneficiary_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanOrderJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beneficiary_id", nullable = false)
    private BeneficiaryJpaEntity beneficiary;

    @Column(nullable = false)
    private boolean forSelf;

    @Column(length = 150)
    private String requesterName;

    @Column(length = 60)
    private String requesterRelationship;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EquipmentType equipmentType;

    @Column(columnDefinition = "TEXT")
    private String needReason; // LGPD: dado sensível

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(length = 500)
    private String rejectionReason;

    @Column(nullable = false)
    private boolean assistedRegistration;

    @Column(length = 50)
    private String decidedBy;

    private LocalDateTime decidedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
