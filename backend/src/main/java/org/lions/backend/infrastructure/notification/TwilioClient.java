package org.lions.backend.infrastructure.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Cliente mínimo da API de mensagens da Twilio (SMS e WhatsApp), sem SDK: {@code POST
 * /2010-04-01/Accounts/{sid}/Messages.json} com {@code To}, {@code From} e {@code Body}. Credenciais em
 * {@code lions.notification.twilio.*} (use variáveis de ambiente; nunca versione o token).
 */
@Component
public class TwilioClient {

    private final RestClient restClient;
    private final String accountSid;
    private final boolean hasCredentials;

    public TwilioClient(RestClient.Builder builder,
                        @Value("${lions.notification.twilio.base-url:https://api.twilio.com}") String baseUrl,
                        @Value("${lions.notification.twilio.account-sid:}") String accountSid,
                        @Value("${lions.notification.twilio.auth-token:}") String authToken) {
        this.accountSid = accountSid == null ? "" : accountSid.trim();
        String token = authToken == null ? "" : authToken.trim();
        this.hasCredentials = !this.accountSid.isEmpty() && !token.isEmpty();
        String basic = Base64.getEncoder()
                .encodeToString((this.accountSid + ":" + token).getBytes(StandardCharsets.UTF_8));
        this.restClient = builder.baseUrl(baseUrl).defaultHeader("Authorization", "Basic " + basic).build();
    }

    public boolean hasCredentials() {
        return hasCredentials;
    }

    /** Envia a mensagem; erro HTTP do provedor vira exceção (RN48 — retentativa). */
    public void sendMessage(String from, String to, String body) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", to);
        form.add("From", from);
        form.add("Body", body);
        restClient.post()
                .uri("/2010-04-01/Accounts/{sid}/Messages.json", accountSid)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }
}
