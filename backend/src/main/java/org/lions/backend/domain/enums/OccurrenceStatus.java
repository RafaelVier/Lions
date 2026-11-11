package org.lions.backend.domain.enums;

public enum OccurrenceStatus {
    ABERTA("Aberta"),
    RESOLVIDA("Resolvida");

    private final String description;

    OccurrenceStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
