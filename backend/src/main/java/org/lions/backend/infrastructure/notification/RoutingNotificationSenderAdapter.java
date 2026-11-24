package org.lions.backend.infrastructure.notification;

import lombok.extern.slf4j.Slf4j;
import org.lions.backend.application.port.NotificationSenderPort;
import org.lions.backend.application.support.LgpdMasker;
import org.lions.backend.domain.enums.NotificationChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Envio das notificações ao beneficiário (RN47/RN48), conforme {@code lions.notification.mode}:
 * <ul>
 *   <li>{@code auto} (padrão): envia pelo provedor configurado para o canal (e-mail por SMTP; SMS e WhatsApp pela
 *   Twilio). Canal sem provedor configurado → tarefa de contato manual com link wa.me (contingência);</li>
 *   <li>{@code manual}: nunca envia — toda notificação vira tarefa de contato manual;</li>
 *   <li>{@code simulate}: só registra no log e considera enviada (desenvolvimento/demonstração).</li>
 * </ul>
 */
@Slf4j
@Component
public class RoutingNotificationSenderAdapter implements NotificationSenderPort {

    public enum Mode { AUTO, MANUAL, SIMULATE }

    private final Mode mode;
    private final Map<NotificationChannel, ChannelSender> senders = new EnumMap<>(NotificationChannel.class);

    public RoutingNotificationSenderAdapter(@Value("${lions.notification.mode:auto}") String mode,
                                            List<ChannelSender> channelSenders) {
        this.mode = parse(mode);
        for (ChannelSender sender : channelSenders) {
            if (sender.isConfigured()) {
                senders.put(sender.channel(), sender);
            }
        }
        log.info("Notificações: modo {}; canais com envio automático: {}", this.mode,
                senders.isEmpty() ? "nenhum (contato manual)" : senders.keySet());
    }

    @Override
    public DeliveryResult send(NotificationChannel channel, String destination, String message) {
        return switch (mode) {
            case MANUAL -> DeliveryResult.MANUAL_REQUIRED;
            case SIMULATE -> {
                log.info("[NOTIFICAÇÃO SIMULADA] canal={} destino={} mensagem={}", channel,
                        mask(channel, destination), message);
                yield DeliveryResult.SENT;
            }
            case AUTO -> {
                ChannelSender sender = senders.get(channel);
                if (sender == null) {
                    yield DeliveryResult.MANUAL_REQUIRED;
                }
                sender.send(destination, message);
                log.debug("Notificação enviada: canal={} destino={}", channel, mask(channel, destination));
                yield DeliveryResult.SENT;
            }
        };
    }

    public Mode mode() {
        return mode;
    }

    public boolean hasAutomaticDelivery(NotificationChannel channel) {
        return mode == Mode.SIMULATE || (mode == Mode.AUTO && senders.containsKey(channel));
    }

    static Mode parse(String value) {
        if (value == null || value.isBlank()) {
            return Mode.AUTO;
        }
        try {
            return Mode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warn("lions.notification.mode inválido ('{}'); usando 'auto'.", value);
            return Mode.AUTO;
        }
    }

    private static String mask(NotificationChannel channel, String destination) {
        if (channel == NotificationChannel.EMAIL) {
            return destination == null || !destination.contains("@") ? "***"
                    : "***" + destination.substring(destination.indexOf('@'));
        }
        return LgpdMasker.maskPhone(destination);
    }
}
