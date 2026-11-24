package org.lions.backend.domain.enums;

public enum OccurrenceType {
    DANO("Dano"),
    INADIMPLENCIA("Inadimplência");

    private final String description;

    OccurrenceType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
