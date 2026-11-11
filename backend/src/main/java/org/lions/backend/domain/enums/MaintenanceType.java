package org.lions.backend.domain.enums;

public enum MaintenanceType {
    CORRETIVA("Corretiva"),
    PREVENTIVA("Preventiva");

    private final String description;

    MaintenanceType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
