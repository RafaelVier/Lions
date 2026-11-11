package org.lions.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.WriteOffReason;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_equipments", indexes = {
        @Index(name = "idx_equipment_status", columnList = "status"),
        @Index(name = "idx_equipment_type", columnList = "type"),
        @Index(name = "idx_equipment_type_status", columnList = "type, status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** {@code PT-%04d}; recebe valor provisório único ({@code TMP-<uuid>}) até o id ser gerado. */
    @Column(nullable = false, unique = true, length = 40)
    private String patrimonyNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private EquipmentType type;

    @Column(name = "equipment_size", length = 40)
    private String size;

    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_condition", nullable = false, length = 20)
    private EquipmentCondition condition;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private EquipmentStatus status;

    @Column(length = 500)
    private String notes;

    @Column(length = 500)
    private String photoUrl;

    private Long reservedForBeneficiaryId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private WriteOffReason writeOffReason;

    @Column(length = 500)
    private String writeOffNotes;

    private LocalDate writeOffDate;

    @Column(length = 50)
    private String writeOffBy;

    private LocalDate lastMaintenanceDate;

    @Column(nullable = false)
    private int loanCyclesSinceMaintenance;

    @Version
    private Long version;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
