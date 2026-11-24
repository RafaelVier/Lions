package org.lions.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.lions.backend.domain.enums.SyncConflictStatus;
import org.lions.backend.domain.enums.SyncOperationType;
import org.lions.backend.domain.enums.SyncPrevailing;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_sync_conflicts", indexes = {
        @Index(name = "idx_sync_conflict_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncConflictJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long equipmentId;

    private Long existingLoanId;

    @Column(nullable = false, length = 64)
    private String clientOperationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SyncOperationType operationType;

    @Column(nullable = false)
    private LocalDateTime incomingOccurredAt;

    private LocalDateTime existingOccurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SyncPrevailing prevailing;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SyncConflictStatus status;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(length = 1000)
    private String resolutionNote;

    @Column(length = 50)
    private String resolvedBy;

    private LocalDateTime resolvedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
