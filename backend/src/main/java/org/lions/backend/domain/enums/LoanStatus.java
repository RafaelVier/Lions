package org.lions.backend.domain.enums;

public enum LoanStatus {
    ATIVO("Ativo"),
    RENOVADO("Renovado"),
    DEVOLVIDO("Devolvido"),
    ATRASADO("Atrasado");

    private final String description;

    LoanStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
