package org.lions.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.lions.backend.domain.enums.ReturnCondition;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_loan_returns", indexes = {
        @Index(name = "idx_loan_return_date", columnList = "return_date")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanReturnJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false, unique = true, updatable = false)
    private LoanJpaEntity loan;

    @Column(name = "return_date", nullable = false, updatable = false)
    private LocalDate returnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private ReturnCondition returnCondition;

    @Column(length = 1000, updatable = false)
    private String damageDescription;

    @Column(length = 500, updatable = false)
    private String returnPhotoUrl;

    @Column(nullable = false, updatable = false)
    private boolean hasDamage;

    @Column(length = 50, updatable = false)
    private String receivedBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
