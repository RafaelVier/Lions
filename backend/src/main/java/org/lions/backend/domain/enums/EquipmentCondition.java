package org.lions.backend.domain.enums;

public enum EquipmentCondition {
    NOVO("Novo"),
    SEMINOVO("Seminovo"),
    USADO("Usado"),
    EM_MANUTENCAO("Em Manutenção"),
    BAIXADO("Baixado");

    private final String description;

    EquipmentCondition(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
