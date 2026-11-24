package org.lions.backend.domain.enums;

public enum EquipmentCondition {
    BOM("Bom"),
    REGULAR("Regular"),
    RUIM("Ruim");

    private final String description;

    EquipmentCondition(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
