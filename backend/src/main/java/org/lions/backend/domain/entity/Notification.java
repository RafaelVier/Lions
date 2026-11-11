package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lions.backend.domain.enums.NotificationChannel;
import org.lions.backend.domain.enums.NotificationStatus;
import org.lions.backend.domain.enums.NotificationType;
import org.lions.backend.domain.enums.RecipientType;
import org.lions.backend.domain.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Notificação a beneficiário ou administradores (RN46–RN49). {@code channel == null} = in-app/manual. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    private Long id;
    private RecipientType recipientType;
    private Long beneficiaryId;
    private Long loanId;
    private NotificationChannel channel;
    private NotificationType type;
    private String message;
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDENTE;
    private int attempts;
    private String lastError;
    private String waLink;
    /** Data de referência para deduplicação (RN46). */
    private LocalDate referenceDate;
    private LocalDateTime sentAt;
    private String resolvedBy;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void markSent(LocalDateTime now) {
        this.attempts++;
        this.status = NotificationStatus.ENVIADA;
        this.sentAt = now;
        this.lastError = null;
    }

    /** RN48 — registra falha; ao atingir {@code maxAttempts} passa a AGUARDANDO_CONTATO_MANUAL. */
    public void registerFailure(String error, int maxAttempts) {
        this.attempts++;
        this.lastError = error;
        this.status = attempts >= maxAttempts
                ? NotificationStatus.AGUARDANDO_CONTATO_MANUAL
                : NotificationStatus.FALHA;
    }

    public void completeManually(String by, LocalDateTime now) {
        if (status == NotificationStatus.ENVIADA || status == NotificationStatus.CONCLUIDA_MANUALMENTE) {
            throw new BusinessException("Notificação já foi concluída.");
        }
        this.status = NotificationStatus.CONCLUIDA_MANUALMENTE;
        this.resolvedBy = by;
        this.resolvedAt = now;
    }
}
