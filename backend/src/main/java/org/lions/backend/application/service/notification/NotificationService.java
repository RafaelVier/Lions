package org.lions.backend.application.service.notification;

import org.lions.backend.domain.enums.NotificationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Contrato §6.2 — dono: Fila + Notificações (Frente E). */
@Service
public class NotificationService {

    /** Registra/envia notificação ao beneficiário pelos canais consentidos (RN07, RN46–RN48). */
    @Transactional
    public void notifyBeneficiary(Long beneficiaryId, Long loanId, NotificationType type, String message) {
        throw new UnsupportedOperationException("Implementado na Frente E");
    }

    /** Notifica os administradores (RN39, RN49). */
    @Transactional
    public void notifyAdmins(NotificationType type, String message) {
        throw new UnsupportedOperationException("Implementado na Frente E");
    }
}
