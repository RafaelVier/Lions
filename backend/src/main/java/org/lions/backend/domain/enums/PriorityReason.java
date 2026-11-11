package org.lions.backend.domain.enums;

public enum PriorityReason {
    URGENCIA_SAUDE("Urgência de saúde"),
    PESSOA_IDOSA("Pessoa idosa"),
    POS_OPERATORIO("Pós-operatório");

    private final String description;

    PriorityReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
