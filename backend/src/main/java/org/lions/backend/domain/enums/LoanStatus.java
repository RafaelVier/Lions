package org.lions.backend.domain.enums;

import java.util.EnumSet;
import java.util.Set;

public enum LoanStatus {
    PENDENTE_ASSINATURA("Pendente de assinatura"),
    ATIVO("Ativo"),
    ATRASADO("Atrasado"),
    DEVOLVIDO("Devolvido"),
    CANCELADO("Cancelado");

    private final String description;

    LoanStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isOpen() {
        return this == PENDENTE_ASSINATURA || this == ATIVO || this == ATRASADO;
    }

    public static Set<LoanStatus> openStatuses() {
        return EnumSet.of(PENDENTE_ASSINATURA, ATIVO, ATRASADO);
    }
}
