package org.lions.backend.domain.enums;

public enum EquipmentType {
    CADEIRA_DE_RODAS("Cadeira de Rodas"),
    CADEIRA_DE_BANHO("Cadeira de Banho"),
    ANDADOR("Andador"),
    MULETAS("Muletas");

    private final String description;

    EquipmentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
