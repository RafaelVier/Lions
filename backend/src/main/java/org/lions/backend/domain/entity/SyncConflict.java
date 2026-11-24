package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lions.backend.domain.enums.SyncConflictStatus;
import org.lions.backend.domain.enums.SyncOperationType;
import org.lions.backend.domain.enums.SyncPrevailing;
import org.lions.backend.domain.exception.BusinessException;

import java.time.LocalDateTime;

/**
 * Conflito de sincronização (RN60): operação offline incompatível com um registro já existente sobre o mesmo
 * equipamento. A operação recebida não é aplicada; {@code prevailing} indica qual registro deve prevalecer pela regra
 * da data/hora mais antiga. Fica pendente até a conferência manual.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncConflict {

    private Long id;
    private Long equipmentId;
    private Long existingLoanId;
    private String clientOperationId;
    private SyncOperationType operationType;
    private LocalDateTime incomingOccurredAt;
    private LocalDateTime existingOccurredAt;
    private SyncPrevailing prevailing;
    private String description;
    @Builder.Default
    private SyncConflictStatus status = SyncConflictStatus.PENDENTE;
    private String username;
    private String resolutionNote;
    private String resolvedBy;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;

    /** Data/hora mais antiga prevalece; empate favorece o registro já existente. */
    public static SyncPrevailing prevailing(LocalDateTime existingAt, LocalDateTime incomingAt) {
        return existingAt == null || incomingAt == null || !incomingAt.isBefore(existingAt)
                ? SyncPrevailing.EXISTENTE
                : SyncPrevailing.RECEBIDA;
    }

    public void resolve(String note, String by, LocalDateTime now) {
        if (status == SyncConflictStatus.RESOLVIDO) {
            throw new BusinessException("Conflito já resolvido.");
        }
        if (note == null || note.isBlank()) {
            throw new BusinessException("Descreva como o conflito foi resolvido.");
        }
        this.status = SyncConflictStatus.RESOLVIDO;
        this.resolutionNote = note.trim();
        this.resolvedBy = by;
        this.resolvedAt = now;
    }
}
