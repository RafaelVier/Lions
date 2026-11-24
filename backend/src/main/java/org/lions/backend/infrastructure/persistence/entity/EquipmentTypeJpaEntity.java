package org.lions.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.lions.backend.domain.enums.EquipmentType;

import java.time.LocalDateTime;

/** Tipo de equipamento (MER: {@code tb_equipamento_tipo}); a chave é o código do enum {@link EquipmentType}. */
@Entity
@Table(name = "tb_equipment_types")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentTypeJpaEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "code", length = 40)
    private EquipmentType code;

    @Column(nullable = false, length = 80)
    private String description;

    private Integer loanDays;

    @Column(length = 50)
    private String updatedBy;

    private LocalDateTime updatedAt;
}
