package org.lions.backend.domain.enums;

public enum NotificationType {
    LEMBRETE_7_DIAS("Lembrete 7 dias antes do vencimento"),
    LEMBRETE_1_DIA("Lembrete 1 dia antes do vencimento"),
    VENCIMENTO_HOJE("Vencimento hoje"),
    ATRASO("Atraso"),
    RESERVA_DISPONIVEL("Reserva disponível"),
    INADIMPLENCIA("Inadimplência"),
    RESUMO_DIARIO("Resumo diário"),
    CONTATO_MANUAL("Contato manual"),
    PREVENTIVA_VENCIDA("Manutenção preventiva vencida"),
    GERAL("Geral");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
