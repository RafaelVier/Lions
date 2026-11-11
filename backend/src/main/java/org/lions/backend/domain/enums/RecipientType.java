package org.lions.backend.domain.enums;

public enum RecipientType {
    BENEFICIARIO("Beneficiário"),
    ADMINISTRADOR("Administrador");

    private final String description;

    RecipientType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
