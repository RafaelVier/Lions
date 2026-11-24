package org.lions.backend.domain.enums;

public enum OrderStatus {
    PENDENTE_ANALISE("Pendente de análise"),
    APROVADO("Aprovado"),
    REJEITADO("Rejeitado"),
    EM_FILA_ESPERA("Em fila de espera"),
    CANCELADO("Cancelado");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
