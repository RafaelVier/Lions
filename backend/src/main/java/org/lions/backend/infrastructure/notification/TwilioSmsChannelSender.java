package org.lions.backend.infrastructure.notification;

import org.lions.backend.application.support.WhatsAppMessageHelper;
import org.lions.backend.domain.enums.NotificationChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** SMS pela Twilio. Ativo com credenciais e {@code lions.notification.twilio.sms-from} (número E.164). */
@Component
public class TwilioSmsChannelSender implements ChannelSender {

    private final TwilioClient client;
    private final WhatsAppMessageHelper phones;
    private final String from;

    public TwilioSmsChannelSender(TwilioClient client, WhatsAppMessageHelper phones,
                                  @Value("${lions.notification.twilio.sms-from:}") String from) {
        this.client = client;
        this.phones = phones;
        this.from = from == null ? "" : from.trim();
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public boolean isConfigured() {
        return client.hasCredentials() && !from.isEmpty();
    }

    @Override
    public void send(String destination, String message) {
        client.sendMessage(from, "+" + phones.sanitizePhone(destination), message);
    }
}
