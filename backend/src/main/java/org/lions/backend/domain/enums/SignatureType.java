package org.lions.backend.domain.enums;

public enum SignatureType {
    FISICA_DIGITALIZADA("Assinatura física digitalizada"),
    DIGITAL("Assinatura digital");

    private final String description;

    SignatureType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
