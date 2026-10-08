package org.lions.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.LoanStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_loans")
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private LoanOrderJpaEntity order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private EquipmentJpaEntity equipment;

    @Column(nullable = false)
    private LocalDate loanDate;

    @Column(nullable = false)
    private LocalDate initialExpectedReturnDate;

    @Column(nullable = false)
    private LocalDate currentDueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LoanStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EquipmentCondition initialCondition;

    @Column(length = 500)
    private String deliveryPhotoUrl;

    @Column(length = 500)
    private String deliveryNotes;

    @Column(nullable = false)
    private int renewalCount;

    private LocalDate lastRenewalDate;

    @Column(length = 300)
    private String termPdfPath;

    @Column(length = 64)
    private String termHashSha256;

    private LocalDateTime electronicAcceptanceDate;

    @Column(length = 120)
    private String acceptedByName;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
