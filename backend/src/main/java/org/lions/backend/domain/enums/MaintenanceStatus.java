package org.lions.backend.domain.enums;

public enum MaintenanceStatus {
    EM_ANDAMENTO("Em Andamento"),
    CONCLUIDA("Concluída"),
    IRRECUPERAVEL("Irrecuperável");

    private final String description;

    MaintenanceStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
