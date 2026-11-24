package org.lions.backend.domain.enums;

public enum NotificationChannel {
    WHATSAPP("WhatsApp"),
    SMS("SMS"),
    EMAIL("E-mail");

    private final String description;

    NotificationChannel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
