package org.lions.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.lions.backend.domain.enums.AuditAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_audit_logs", indexes = {
        @Index(name = "idx_audit_entity", columnList = "entity_name, entity_id"),
        @Index(name = "idx_audit_occurred_at", columnList = "occurred_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_name", nullable = false, length = 60, updatable = false)
    private String entityName;

    @Column(name = "entity_id", length = 100, updatable = false)
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private AuditAction action;

    @Column(nullable = false, length = 50, updatable = false)
    private String username;

    @Column(nullable = false, updatable = false)
    private boolean automatic;

    @Column(length = 30, updatable = false)
    private String originRule;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String details;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;
}
