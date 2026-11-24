package org.lions.backend.domain.enums;

public enum MaintenanceVerdict {
    APTO("Apto"),
    INSERVIVEL("Inservível");

    private final String description;

    MaintenanceVerdict(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
