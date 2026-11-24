package org.lions.backend.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Linha de {@code tb_equipment_specifications} (MER: tb_especificacao). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentSpecificationEmbeddable {

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "spec_value", nullable = false, length = 255)
    private String value;
}
