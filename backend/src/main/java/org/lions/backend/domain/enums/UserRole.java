package org.lions.backend.domain.enums;

public enum UserRole {
    ROLE_ADMIN("Administrador"),
    ROLE_OPERATOR("Operador"),
    ROLE_VIEWER("Consulta");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
