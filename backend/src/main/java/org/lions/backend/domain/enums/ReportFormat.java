package org.lions.backend.domain.enums;

public enum ReportFormat {
    PDF("PDF"),
    CSV("CSV");

    private final String description;

    ReportFormat(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
