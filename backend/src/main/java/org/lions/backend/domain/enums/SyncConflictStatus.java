package org.lions.backend.domain.enums;

/** Sincronização offline (RN60). */
public enum SyncConflictStatus {
    PENDENTE("Pendente de conferência"), RESOLVIDO("Resolvido");

    private final String description;

    SyncConflictStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
