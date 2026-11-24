package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lions.backend.domain.enums.OccurrenceStatus;
import org.lions.backend.domain.enums.OccurrenceType;
import org.lions.backend.domain.exception.BusinessException;

import java.time.LocalDateTime;

/** Ocorrência vinculada ao beneficiário (dano — RN40; inadimplência — RN27). Ocorrência aberta bloqueia (RN20). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Occurrence {

    private Long id;
    private Beneficiary beneficiary;
    private Long loanId;
    private OccurrenceType type;
    @Builder.Default
    private OccurrenceStatus status = OccurrenceStatus.ABERTA;
    private String description;
    private boolean automatic;
    private String createdBy;
    private LocalDateTime resolvedAt;
    private String resolvedBy;
    private String resolutionNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void resolve(String note, String by, LocalDateTime now) {
        if (status != OccurrenceStatus.ABERTA) {
            throw new BusinessException("Ocorrência já está resolvida.");
        }
        if (note == null || note.isBlank()) {
            throw new BusinessException("Nota de resolução é obrigatória.");
        }
        this.status = OccurrenceStatus.RESOLVIDA;
        this.resolutionNote = note;
        this.resolvedBy = by;
        this.resolvedAt = now;
    }

    public boolean isOpen() {
        return status == OccurrenceStatus.ABERTA;
    }
}
