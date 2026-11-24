package org.lions.backend.infrastructure.notification;

import org.junit.jupiter.api.Test;
import org.lions.backend.application.port.NotificationSenderPort.DeliveryResult;
import org.lions.backend.application.support.WhatsAppMessageHelper;
import org.lions.backend.domain.enums.NotificationChannel;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class NotificationSendersTest {

    /** Sender de teste que guarda as mensagens enviadas. */
    private static final class FakeSender implements ChannelSender {
        private final NotificationChannel channel;
        private final boolean configured;
        private final List<String> sent = new ArrayList<>();

        FakeSender(NotificationChannel channel, boolean configured) {
            this.channel = channel;
            this.configured = configured;
        }

        @Override
        public NotificationChannel channel() {
            return channel;
        }

        @Override
        public boolean isConfigured() {
            return configured;
        }

        @Override
        public void send(String destination, String message) {
            sent.add(destination + "|" + message);
        }
    }

    @Test
    void deveEnviarPeloProvedorQuandoCanalConfiguradoNoModoAuto() {
        FakeSender email = new FakeSender(NotificationChannel.EMAIL, true);
        FakeSender sms = new FakeSender(NotificationChannel.SMS, false);
        RoutingNotificationSenderAdapter router = new RoutingNotificationSenderAdapter("auto", List.of(email, sms));

        assertThat(router.send(NotificationChannel.EMAIL, "a@b.com", "oi")).isEqualTo(DeliveryResult.SENT);
        assertThat(email.sent).containsExactly("a@b.com|oi");
        // RN47 — canal sem provedor configurado cai na contingência de contato manual
        assertThat(router.send(NotificationChannel.SMS, "45999990000", "oi")).isEqualTo(DeliveryResult.MANUAL_REQUIRED);
        assertThat(router.send(NotificationChannel.WHATSAPP, "45999990000", "oi"))
                .isEqualTo(DeliveryResult.MANUAL_REQUIRED);
        assertThat(sms.sent).isEmpty();
        assertThat(router.hasAutomaticDelivery(NotificationChannel.EMAIL)).isTrue();
        assertThat(router.hasAutomaticDelivery(NotificationChannel.SMS)).isFalse();
    }

    @Test
    void deveRespeitarModosManualESimulado() {
        FakeSender email = new FakeSender(NotificationChannel.EMAIL, true);
        assertThat(new RoutingNotificationSenderAdapter("manual", List.of(email))
                .send(NotificationChannel.EMAIL, "a@b.com", "oi")).isEqualTo(DeliveryResult.MANUAL_REQUIRED);
        assertThat(new RoutingNotificationSenderAdapter("SIMULATE", List.of(email))
                .send(NotificationChannel.SMS, "45999990000", "oi")).isEqualTo(DeliveryResult.SENT);
        assertThat(email.sent).isEmpty();
        assertThat(RoutingNotificationSenderAdapter.parse(null)).isEqualTo(RoutingNotificationSenderAdapter.Mode.AUTO);
        assertThat(RoutingNotificationSenderAdapter.parse("xpto")).isEqualTo(RoutingNotificationSenderAdapter.Mode.AUTO);
    }

    @Test
    void devePropagarFalhaDoProvedorParaRetentativa() {
        ChannelSender failing = new ChannelSender() {
            @Override
            public NotificationChannel channel() {
                return NotificationChannel.EMAIL;
            }

            @Override
            public boolean isConfigured() {
                return true;
            }

            @Override
            public void send(String destination, String message) {
                throw new IllegalStateException("SMTP fora do ar");
            }
        };
        RoutingNotificationSenderAdapter router = new RoutingNotificationSenderAdapter("auto", List.of(failing));
        assertThatThrownBy(() -> router.send(NotificationChannel.EMAIL, "a@b.com", "oi"))
                .hasMessageContaining("SMTP");
    }

    @Test
    @SuppressWarnings("unchecked")
    void deveMontarEmailQuandoSmtpConfigurado() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);
        EmailChannelSender sender = new EmailChannelSender(provider, "banco@lions.org.br");

        assertThat(sender.isConfigured()).isTrue();
        sender.send("maria@exemplo.com", "Seu empréstimo vence amanhã.");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("maria@exemplo.com");
        assertThat(captor.getValue().getFrom()).isEqualTo("banco@lions.org.br");
        assertThat(captor.getValue().getText()).isEqualTo("Seu empréstimo vence amanhã.");

        ObjectProvider<JavaMailSender> empty = mock(ObjectProvider.class);
        assertThat(new EmailChannelSender(empty, "banco@lions.org.br").isConfigured()).isFalse();
        assertThat(new EmailChannelSender(provider, " ").isConfigured()).isFalse();
    }

    @Test
    void deveChamarApiDaTwilioQuandoEnviaWhatsAppESms() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TwilioClient client = new TwilioClient(builder, "https://twilio.test", "AC123", "segredo");
        WhatsAppMessageHelper phones = new WhatsAppMessageHelper();
        String basic = "Basic " + Base64.getEncoder().encodeToString("AC123:segredo".getBytes(StandardCharsets.UTF_8));

        MultiValueMap<String, String> whatsappForm = new LinkedMultiValueMap<>();
        whatsappForm.add("To", "whatsapp:+5545999991234");
        whatsappForm.add("From", "whatsapp:+554530000000");
        whatsappForm.add("Body", "Olá!");
        server.expect(requestTo("https://twilio.test/2010-04-01/Accounts/AC123/Messages.json"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", basic))
                .andExpect(content().formData(whatsappForm))
                .andRespond(withStatus(HttpStatus.CREATED));
        MultiValueMap<String, String> smsForm = new LinkedMultiValueMap<>();
        smsForm.add("To", "+5545999991234");
        smsForm.add("From", "+554530000001");
        smsForm.add("Body", "Olá!");
        server.expect(requestTo("https://twilio.test/2010-04-01/Accounts/AC123/Messages.json"))
                .andExpect(content().formData(smsForm))
                .andRespond(withStatus(HttpStatus.CREATED));

        TwilioWhatsAppChannelSender whatsapp = new TwilioWhatsAppChannelSender(client, phones, "+554530000000");
        TwilioSmsChannelSender sms = new TwilioSmsChannelSender(client, phones, "+554530000001");
        assertThat(whatsapp.isConfigured()).isTrue();
        assertThat(sms.isConfigured()).isTrue();
        whatsapp.send("(45) 99999-1234", "Olá!");
        sms.send("45 99999-1234", "Olá!");
        server.verify();
    }

    @Test
    void deveLancarExcecaoQuandoTwilioRetornaErro() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TwilioClient client = new TwilioClient(builder, "https://twilio.test", "AC123", "segredo");
        server.expect(requestTo("https://twilio.test/2010-04-01/Accounts/AC123/Messages.json"))
                .andRespond(withServerError());

        TwilioSmsChannelSender sms = new TwilioSmsChannelSender(client, new WhatsAppMessageHelper(), "+554530000001");
        assertThatThrownBy(() -> sms.send("45999991234", "Olá!")).isInstanceOf(RestClientException.class);
    }

    @Test
    void naoDeveConfigurarTwilioQuandoSemCredenciais() {
        TwilioClient client = new TwilioClient(RestClient.builder(), "https://twilio.test", "", "");
        assertThat(new TwilioSmsChannelSender(client, new WhatsAppMessageHelper(), "+554530000001").isConfigured())
                .isFalse();
        assertThat(new TwilioWhatsAppChannelSender(client, new WhatsAppMessageHelper(), "").isConfigured())
                .isFalse();
    }
}
