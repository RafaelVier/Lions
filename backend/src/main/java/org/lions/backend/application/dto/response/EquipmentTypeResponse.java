package org.lions.backend.application.dto.response;

import org.lions.backend.domain.enums.EquipmentType;

import java.time.LocalDateTime;

/**
 * @param loanDays          prazo configurado para o tipo ({@code null} = usa o padrão)
 * @param effectiveLoanDays prazo efetivamente aplicado aos novos empréstimos e renovações (RN18)
 */
public record EquipmentTypeResponse(
        EquipmentType code,
        String description,
        Integer loanDays,
        int effectiveLoanDays,
        String updatedBy,
        LocalDateTime updatedAt) {
}
