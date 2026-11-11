package org.lions.backend.domain.enums;

public enum WaitlistStatus {
    AGUARDANDO("Aguardando"),
    RESERVADO("Reservado"),
    ATENDIDO("Atendido"),
    REMOVIDO("Removido");

    private final String description;

    WaitlistStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
