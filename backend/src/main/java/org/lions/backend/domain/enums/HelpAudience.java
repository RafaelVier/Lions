package org.lions.backend.domain.enums;

public enum HelpAudience {
    PUBLICO("Público"),
    ADMIN("Administrador"),
    OPERADOR("Operador"),
    CONSULTA("Consulta");

    private final String description;

    HelpAudience(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
