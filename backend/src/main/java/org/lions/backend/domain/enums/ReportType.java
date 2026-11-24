package org.lions.backend.domain.enums;

public enum ReportType {
    INVENTARIO("Inventário"),
    EMPRESTIMOS("Empréstimos"),
    ASSISTIDOS("Assistidos"),
    MANUTENCOES("Manutenções");

    private final String description;

    ReportType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
