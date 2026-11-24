package org.lions.backend.domain.enums;

public enum WriteOffReason {
    INSERVIVEL("Inservível"),
    PERDA("Perda"),
    FURTO("Furto"),
    DOACAO("Doação");

    private final String description;

    WriteOffReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
