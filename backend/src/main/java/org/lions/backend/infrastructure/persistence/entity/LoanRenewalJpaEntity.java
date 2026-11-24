package org.lions.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_loan_renewals", indexes = {
        @Index(name = "idx_loan_renewal_loan", columnList = "loan_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanRenewalJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false, updatable = false)
    private Long loanId;

    @Column(nullable = false, updatable = false)
    private LocalDate previousDueDate;

    @Column(nullable = false, updatable = false)
    private LocalDate newDueDate;

    @Column(length = 50, updatable = false)
    private String renewedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime renewedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
