package org.lions.backend.application.dto.response;

import org.lions.backend.domain.entity.Beneficiary;
import org.lions.backend.domain.entity.Notification;
import org.lions.backend.domain.enums.NotificationChannel;
import org.lions.backend.domain.enums.NotificationStatus;
import org.lions.backend.domain.enums.NotificationType;
import org.lions.backend.domain.enums.RecipientType;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Notificação (RN46–RN49). {@code channel == null} = notificação interna (admin) ou contato manual.
 * {@code waLink} traz o link wa.me com a mensagem pronta para o contato manual em 1 toque.
 */
public record NotificationResponse(
        Long id,
        RecipientType recipientType,
        Long beneficiaryId,
        String beneficiaryName,
        String beneficiaryPhone,
        Long loanId,
        NotificationChannel channel,
        NotificationType type,
        String typeDescription,
        String message,
        NotificationStatus status,
        String statusDescription,
        int attempts,
        String lastError,
        String waLink,
        LocalDate referenceDate,
        LocalDateTime sentAt,
        String resolvedBy,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt) {

    public static NotificationResponse from(Notification n, Beneficiary beneficiary) {
        return new NotificationResponse(
                n.getId(),
                n.getRecipientType(),
                n.getBeneficiaryId(),
                beneficiary != null ? beneficiary.getFullName() : null,
                beneficiary != null ? beneficiary.getPhone() : null,
                n.getLoanId(),
                n.getChannel(),
                n.getType(),
                n.getType() != null ? n.getType().getDescription() : null,
                n.getMessage(),
                n.getStatus(),
                n.getStatus() != null ? n.getStatus().getDescription() : null,
                n.getAttempts(),
                n.getLastError(),
                n.getWaLink(),
                n.getReferenceDate(),
                n.getSentAt(),
                n.getResolvedBy(),
                n.getResolvedAt(),
                n.getCreatedAt());
    }
}
