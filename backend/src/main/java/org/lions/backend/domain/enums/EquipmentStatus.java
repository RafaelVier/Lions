package org.lions.backend.domain.enums;

public enum EquipmentStatus {
    DISPONIVEL("Disponível"),
    EMPRESTADO("Emprestado"),
    EM_MANUTENCAO("Em Manutenção"),
    BAIXADO("Baixado");

    private final String description;

    EquipmentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
