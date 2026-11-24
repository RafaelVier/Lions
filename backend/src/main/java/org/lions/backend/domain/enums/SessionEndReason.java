package org.lions.backend.domain.enums;

/** Como a sessão (token) terminou (RN57). */
public enum SessionEndReason {
    LOGOUT("Logout"),
    RENOVADA("Substituída por token renovado"),
    EXPIRADA("Expirada por inatividade"),
    REVOGADA("Revogada (usuário inativado)");

    private final String description;

    SessionEndReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
