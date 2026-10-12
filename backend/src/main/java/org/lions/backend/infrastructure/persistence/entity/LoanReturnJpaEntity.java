package org.lions.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.lions.backend.domain.enums.ReturnCondition;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_loan_returns")
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private LoanJpaEntity loan;

    @Column(nullable = false)
    private LocalDate returnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReturnCondition returnCondition;

    @Column(length = 500)
    private String damageDescription;

    @Column(length = 500)
    private String returnPhotoUrl;

    @Column(nullable = false)
    private boolean hasDamage;

    @Column(nullable = false)
    private boolean responsibilityAssigned;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
