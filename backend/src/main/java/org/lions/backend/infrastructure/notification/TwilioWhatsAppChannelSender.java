package org.lions.backend.infrastructure.notification;

import org.lions.backend.application.support.WhatsAppMessageHelper;
import org.lions.backend.domain.enums.NotificationChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * WhatsApp pela Twilio (WhatsApp Business). Ativo com credenciais e {@code lions.notification.twilio.whatsapp-from}
 * (número habilitado na Twilio, ex.: {@code +554599990000}). Mensagens fora da janela de 24 h exigem template
 * aprovado na Meta — sem ele a Twilio recusa e a notificação cai em retentativa/contato manual (RN48).
 */
@Component
public class TwilioWhatsAppChannelSender implements ChannelSender {

    private static final String PREFIX = "whatsapp:";

    private final TwilioClient client;
    private final WhatsAppMessageHelper phones;
    private final String from;

    public TwilioWhatsAppChannelSender(TwilioClient client, WhatsAppMessageHelper phones,
                                       @Value("${lions.notification.twilio.whatsapp-from:}") String from) {
        this.client = client;
        this.phones = phones;
        this.from = from == null ? "" : from.trim();
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.WHATSAPP;
    }

    @Override
    public boolean isConfigured() {
        return client.hasCredentials() && !from.isEmpty();
    }

    @Override
    public void send(String destination, String message) {
        String sender = from.startsWith(PREFIX) ? from : PREFIX + from;
        client.sendMessage(sender, PREFIX + "+" + phones.sanitizePhone(destination), message);
    }
}
