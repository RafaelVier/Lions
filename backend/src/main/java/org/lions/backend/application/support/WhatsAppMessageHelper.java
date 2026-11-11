package org.lions.backend.application.support;

import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Montagem de links {@code wa.me} e mensagens padrão de WhatsApp (RN46–RN48).
 * Os prazos são sempre informados por data — nunca por quantidade fixa de dias.
 */
@Component
public class WhatsAppMessageHelper {

    private static final String WA_BASE_URL = "https://wa.me/";
    private static final String BRAZIL_COUNTRY_CODE = "55";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
    private static final String SIGNATURE = "\n\nBanco Ortopédico — Lions Clube de Medianeira.";

    /** Mantém só dígitos e acrescenta o DDI 55 quando o número tiver DDD + telefone (10/11 dígitos). */
    public String sanitizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == 10 || digits.length() == 11) {
            return BRAZIL_COUNTRY_CODE + digits;
        }
        return digits;
    }

    /** {@code https://wa.me/<telefone>?text=<mensagem codificada>}. */
    public String buildLink(String phone, String message) {
        String sanitized = sanitizePhone(phone);
        if (sanitized == null || sanitized.isBlank()) {
            throw new IllegalArgumentException("Telefone obrigatório para gerar link do WhatsApp.");
        }
        String link = WA_BASE_URL + sanitized;
        if (message == null || message.isBlank()) {
            return link;
        }
        return link + "?text=" + URLEncoder.encode(message, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public String dueDateReminderMessage(String beneficiaryName, String equipmentDescription,
                                         String patrimonyNumber, LocalDate dueDate) {
        return "Olá, " + beneficiaryName + "! Lembramos que o prazo de devolução do equipamento "
                + equipment(equipmentDescription, patrimonyNumber) + " vence em " + DATE.format(dueDate) + "."
                + " Caso ainda precise dele, entre em contato para avaliarmos a renovação." + SIGNATURE;
    }

    public String overdueMessage(String beneficiaryName, String equipmentDescription,
                                 String patrimonyNumber, LocalDate dueDate) {
        return "Olá, " + beneficiaryName + ". O prazo de devolução do equipamento "
                + equipment(equipmentDescription, patrimonyNumber) + " venceu em " + DATE.format(dueDate) + "."
                + " Por favor, entre em contato para combinarmos a devolução ou a regularização." + SIGNATURE;
    }

    public String reservationAvailableMessage(String beneficiaryName, String equipmentDescription,
                                              String patrimonyNumber, LocalDateTime reservationExpiresAt) {
        return "Olá, " + beneficiaryName + "! O equipamento " + equipment(equipmentDescription, patrimonyNumber)
                + " que você aguardava está reservado para você até " + DATE_TIME.format(reservationExpiresAt) + "."
                + " Entre em contato para combinar a retirada." + SIGNATURE;
    }

    private String equipment(String description, String patrimonyNumber) {
        if (patrimonyNumber == null || patrimonyNumber.isBlank()) {
            return description;
        }
        return description + " (" + patrimonyNumber + ")";
    }
}
