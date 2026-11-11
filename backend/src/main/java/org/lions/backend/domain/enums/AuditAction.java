package org.lions.backend.domain.enums;

public enum AuditAction {
    CREATE("Criação"),
    UPDATE("Alteração"),
    INACTIVATE("Inativação"),
    REACTIVATE("Reativação"),
    STATUS_CHANGE("Mudança de status"),
    CANCEL("Cancelamento"),
    ANONYMIZE("Anonimização"),
    ADJUSTMENT("Ajuste"),
    LOGIN("Login"),
    LOGOUT("Logout"),
    EXPORT("Exportação");

    private final String description;

    AuditAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
