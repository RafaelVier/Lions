package org.lions.backend.infrastructure.notification;

import org.lions.backend.domain.enums.NotificationChannel;

/** Envio por um canal concreto (provedor real). Só é usado quando {@link #isConfigured()}. */
public interface ChannelSender {

    NotificationChannel channel();

    boolean isConfigured();

    /**
     * Entrega a mensagem ao provedor.
     *
     * @throws RuntimeException em falha (conta como tentativa — RN48)
     */
    void send(String destination, String message);
}
