package org.lions.backend.domain.enums;

public enum NotificationStatus {
    PENDENTE("Pendente"),
    ENVIADA("Enviada"),
    FALHA("Falha"),
    AGUARDANDO_CONTATO_MANUAL("Aguardando contato manual"),
    CONCLUIDA_MANUALMENTE("Concluída manualmente");

    private final String description;

    NotificationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
