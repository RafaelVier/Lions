package org.lions.backend.domain.enums;

public enum CostType {
    PECA("Peça"),
    SERVICO("Serviço");

    private final String description;

    CostType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
