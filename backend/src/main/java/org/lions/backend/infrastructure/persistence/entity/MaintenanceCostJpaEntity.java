package org.lions.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.lions.backend.domain.enums.CostType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_maintenance_costs", indexes = {
        @Index(name = "idx_maintenance_cost_maintenance", columnList = "maintenance_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceCostJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maintenance_id", nullable = false, updatable = false)
    private MaintenanceJpaEntity maintenance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private CostType type;

    @Column(nullable = false, length = 500, updatable = false)
    private String description;

    @Column(length = 150, updatable = false)
    private String partReplaced;

    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false)
    private boolean adjustment;

    @Column(length = 500, updatable = false)
    private String adjustmentReason;

    @Column(length = 50, updatable = false)
    private String registeredBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime registeredAt;
}
