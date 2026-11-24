package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.lions.backend.domain.enums.NotificationChannel;

import java.util.Set;

/**
 * Renovação do consentimento LGPD e dos canais autorizados (RN07, RN47). A data/hora é registrada pelo sistema.
 * Para revogar o consentimento, solicite a anonimização (RN59).
 */
public record ConsentRequest(
        @NotNull(message = "O consentimento é obrigatório.")
        @AssertTrue(message = "O consentimento deve ser concedido (para revogar, solicite a anonimização).")
        Boolean consentGranted,
        Set<NotificationChannel> notificationChannels,
        @Size(max = 150, message = "O nome de quem consentiu deve ter no máximo 150 caracteres.")
        String consentGivenBy) {
}
