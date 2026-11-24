package org.lions.backend.domain.enums;

/** Sincronização offline (RN60). */
public enum SyncOperationType {
    RETIRADA("Retirada"), DEVOLUCAO("Devolução"), RENOVACAO("Renovação");

    private final String description;

    SyncOperationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
