package org.lions.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.lions.backend.domain.enums.SyncOperationType;
import org.lions.backend.domain.enums.SyncResultStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_sync_operations", indexes = {
        @Index(name = "idx_sync_operation_loan", columnList = "resulting_loan_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncOperationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String clientOperationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SyncOperationType type;

    private Long beneficiaryId;

    private Long equipmentId;

    private Long loanId;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    @Column(nullable = false, length = 50)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SyncResultStatus result;

    @Column(length = 1000)
    private String message;

    @Column(name = "resulting_loan_id")
    private Long resultingLoanId;

    private Long conflictId;
}
