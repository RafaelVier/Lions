package org.lions.backend.domain.enums;

public enum MaintenanceStatus {
    EM_ANDAMENTO("Em andamento"),
    CONCLUIDA_APTO("Concluída - apto"),
    CONCLUIDA_INSERVIVEL("Concluída - inservível");

    private final String description;

    MaintenanceStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
