package org.lions.backend.domain.enums;

/** Sincronização offline (RN60). */
public enum SyncResultStatus {
    APLICADA("Aplicada"), RECUSADA("Recusada"), CONFLITO("Conflito"), DUPLICADA("Já processada");

    private final String description;

    SyncResultStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
