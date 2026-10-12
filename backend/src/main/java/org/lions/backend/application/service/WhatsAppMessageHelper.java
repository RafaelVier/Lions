package org.lions.backend.application.service;

import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * Utilitário para gerar links wa.me com mensagens pré-formatadas para o Márcio
 * enviar aos beneficiários em 1 único toque pelo celular.
 */
@Component
public class WhatsAppMessageHelper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String generateRenewalReminderLink(String phone, String beneficiaryName, String equipmentDescription, String patrimonyNumber, java.time.LocalDate dueDate) {
        String cleanPhone = sanitizePhone(phone);
        String formattedDate = dueDate.format(DATE_FORMATTER);

        String message = String.format(
                "Olá, %s! Aqui é o Márcio do Lions Clube de Medianeira.\n\n" +
                "Esperamos que esteja bem! Constatamos que o empréstimo do equipamento *%s* (Patrimônio: %s) completará o período de 90 dias no dia *%s*.\n\n" +
                "Gostaríamos de saber: o equipamento ainda está em uso e você deseja renovar o empréstimo por mais 90 dias, ou já tem previsão de devolução para que possamos atender outra pessoa da fila?\n\n" +
                "Muito obrigado e um abraço do Lions Clube de Medianeira!",
                beneficiaryName, equipmentDescription, patrimonyNumber, formattedDate
        );

        return buildWaLink(cleanPhone, message);
    }

    public String generateOverdueReminderLink(String phone, String beneficiaryName, String equipmentDescription, String patrimonyNumber, java.time.LocalDate dueDate) {
        String cleanPhone = sanitizePhone(phone);
        String formattedDate = dueDate.format(DATE_FORMATTER);

        String message = String.format(
                "Olá, %s! Aqui é o Márcio do Lions Clube de Medianeira.\n\n" +
                "Entramos em contato referente ao empréstimo do equipamento *%s* (Patrimônio: %s), cujo prazo de 90 dias venceu em *%s*.\n\n" +
                "Por favor, nos confirme se você ainda necessita do equipamento para registrarmos a renovação no sistema ou se podemos agendar a devolução.\n\n" +
                "Ficamos no aguardo de sua resposta!",
                beneficiaryName, equipmentDescription, patrimonyNumber, formattedDate
        );

        return buildWaLink(cleanPhone, message);
    }

    public String generateApprovalNotificationLink(String phone, String beneficiaryName, String equipmentDescription, String patrimonyNumber) {
        String cleanPhone = sanitizePhone(phone);

        String message = String.format(
                "Olá, %s! Aqui é o Márcio do Lions Clube de Medianeira.\n\n" +
                "Temos uma ótima notícia! Seu pedido de empréstimo para *%s* foi aprovado com sucesso.\n" +
                "Equipamento separado com o patrimônio: *%s*.\n\n" +
                "Podemos combinar a entrega/retirada e o aceite do termo de responsabilidade.",
                beneficiaryName, equipmentDescription, patrimonyNumber
        );

        return buildWaLink(cleanPhone, message);
    }

    private String sanitizePhone(String phone) {
        if (phone == null) return "";
        String clean = phone.replaceAll("\\D", "");
        if (!clean.startsWith("55") && clean.length() >= 10) {
            clean = "55" + clean;
        }
        return clean;
    }

    private String buildWaLink(String cleanPhone, String message) {
        String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
        return "https://wa.me/" + cleanPhone + "?text=" + encodedMessage;
    }
}
