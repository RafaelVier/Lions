package org.lions.backend.application.port;

import org.lions.backend.domain.enums.NotificationChannel;

/**
 * Envio de notificações ao beneficiário por um canal (RN47/RN48). Provedores reais (WhatsApp Business, SMS, e-mail)
 * ficam fora do escopo: o adaptador padrão apenas registra em log ou devolve a tarefa para contato manual.
 */
public interface NotificationSenderPort {

    enum DeliveryResult {
        /** Mensagem entregue ao provedor do canal. */
        SENT,
        /** Canal sem envio automático configurado: a notificação vira tarefa de contato manual (link wa.me). */
        MANUAL_REQUIRED
    }

    /**
     * Envia {@code message} para {@code destination} (telefone ou e-mail) pelo canal informado.
     *
     * @throws RuntimeException em falha de envio (conta como tentativa — RN48)
     */
    DeliveryResult send(NotificationChannel channel, String destination, String message);
}
