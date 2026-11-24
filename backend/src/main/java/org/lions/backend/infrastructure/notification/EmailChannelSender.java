package org.lions.backend.infrastructure.notification;

import org.lions.backend.domain.enums.NotificationChannel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * E-mail por SMTP (Spring Mail). Ativo quando há {@code spring.mail.host} configurado (o Spring cria o
 * {@link JavaMailSender}) e um remetente em {@code lions.notification.email.from}.
 */
@Component
public class EmailChannelSender implements ChannelSender {

    static final String SUBJECT = "Banco Ortopédico — Lions Clube de Medianeira";

    private final JavaMailSender mailSender;
    private final String from;

    public EmailChannelSender(ObjectProvider<JavaMailSender> mailSender,
                              @Value("${lions.notification.email.from:}") String from) {
        this.mailSender = mailSender.getIfAvailable();
        this.from = from == null ? "" : from.trim();
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public boolean isConfigured() {
        return mailSender != null && !from.isEmpty();
    }

    @Override
    public void send(String destination, String message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(destination);
        mail.setSubject(SUBJECT);
        mail.setText(message);
        mailSender.send(mail);
    }
}
