package org.lions.backend.application.dto.response;

import org.lions.backend.domain.entity.Beneficiary;
import org.lions.backend.domain.entity.Occurrence;
import org.lions.backend.domain.enums.OccurrenceStatus;
import org.lions.backend.domain.enums.OccurrenceType;

import java.time.LocalDateTime;

/** Ocorrência (dano — RN40; inadimplência — RN27). Aberta bloqueia novos empréstimos (RN20). */
public record OccurrenceResponse(
        Long id,
        Long beneficiaryId,
        String beneficiaryName,
        Long loanId,
        OccurrenceType type,
        String typeDescription,
        OccurrenceStatus status,
        String statusDescription,
        String description,
        boolean automatic,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt,
        String resolvedBy,
        String resolutionNote) {

    public static OccurrenceResponse from(Occurrence o) {
        Beneficiary b = o.getBeneficiary();
        return new OccurrenceResponse(
                o.getId(),
                b != null ? b.getId() : null,
                b != null ? b.getFullName() : null,
                o.getLoanId(),
                o.getType(),
                o.getType() != null ? o.getType().getDescription() : null,
                o.getStatus(),
                o.getStatus() != null ? o.getStatus().getDescription() : null,
                o.getDescription(),
                o.isAutomatic(),
                o.getCreatedBy(),
                o.getCreatedAt(),
                o.getResolvedAt(),
                o.getResolvedBy(),
                o.getResolutionNote());
    }
}
