package org.lions.backend.domain.enums;

public enum HelpContentType {
    TUTORIAL("Tutorial"),
    DICA("Dica"),
    FAQ("Pergunta frequente"),
    ONBOARDING("Onboarding");

    private final String description;

    HelpContentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
