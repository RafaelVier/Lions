package org.lions.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.PriorityReason;
import org.lions.backend.domain.enums.WaitlistStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_waitlist_entries", indexes = {
        @Index(name = "idx_waitlist_type_status", columnList = "equipment_type, status"),
        @Index(name = "idx_waitlist_beneficiary", columnList = "beneficiary_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitlistEntryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beneficiary_id", nullable = false)
    private BeneficiaryJpaEntity beneficiary;

    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_type", nullable = false, length = 40)
    private EquipmentType equipmentType;

    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WaitlistStatus status;

    @Column(nullable = false)
    private LocalDateTime queuedAt;

    @Column(nullable = false)
    private LocalDateTime originalRequestedAt;

    @Column(nullable = false)
    private boolean priority;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PriorityReason priorityReason;

    @Column(length = 500)
    private String priorityJustification;

    @Column(length = 50)
    private String priorityAuthorizedBy;

    private Long reservedEquipmentId;

    private LocalDateTime reservedAt;

    private LocalDateTime reservationExpiresAt;

    @Column(nullable = false)
    private int consecutiveExpirations;

    @Column(length = 500)
    private String removedReason;

    private LocalDateTime closedAt;

    @Column(length = 50)
    private String createdBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
