package org.lions.backend.domain.enums;

public enum ReturnCondition {
    BOM_ESTADO("Em bom estado"),
    PRECISA_MANUTENCAO("Precisa de manutenção"),
    DANIFICADO("Danificado");

    private final String description;

    ReturnCondition(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
