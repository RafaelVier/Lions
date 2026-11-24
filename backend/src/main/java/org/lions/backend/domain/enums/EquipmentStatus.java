package org.lions.backend.domain.enums;

import java.util.EnumSet;
import java.util.Set;

public enum EquipmentStatus {
    DISPONIVEL("Disponível"),
    RESERVADO("Reservado"),
    EMPRESTADO("Emprestado"),
    EM_MANUTENCAO("Em manutenção"),
    BAIXADO("Baixado");

    private final String description;

    EquipmentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * RN12 — transições permitidas do ciclo de vida do equipamento. BAIXADO é final.
     */
    public Set<EquipmentStatus> allowedTransitions() {
        return switch (this) {
            case DISPONIVEL -> EnumSet.of(RESERVADO, EMPRESTADO, EM_MANUTENCAO, BAIXADO);
            case RESERVADO -> EnumSet.of(EMPRESTADO, DISPONIVEL);
            case EMPRESTADO -> EnumSet.of(DISPONIVEL, EM_MANUTENCAO);
            case EM_MANUTENCAO -> EnumSet.of(DISPONIVEL, BAIXADO);
            case BAIXADO -> EnumSet.noneOf(EquipmentStatus.class);
        };
    }

    public boolean canTransitionTo(EquipmentStatus target) {
        return target != null && allowedTransitions().contains(target);
    }
}
