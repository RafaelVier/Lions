package org.lions.backend.domain.enums;

public enum OrderStatus {
    PENDENTE_ANALISE("Pendente de Análise"),
    APROVADO("Aprovado"),
    REJEITADO("Rejeitado"),
    PENDENTE_ESTOQUE("Pendente por Falta de Estoque"),
    CANCELADO("Cancelado");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
