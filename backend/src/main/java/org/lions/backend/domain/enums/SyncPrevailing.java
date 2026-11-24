package org.lions.backend.domain.enums;

/** Sincronização offline (RN60). */
public enum SyncPrevailing {
    EXISTENTE("Registro já existente (mais antigo)"), RECEBIDA("Operação offline recebida (mais antiga)");

    private final String description;

    SyncPrevailing(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
