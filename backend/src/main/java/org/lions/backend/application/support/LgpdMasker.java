package org.lions.backend.application.support;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Mascaramento de dados pessoais para listagens, relatórios e perfil Consulta (LGPD — RN45/RN60).
 * Utilitário puro (sem Spring).
 */
public final class LgpdMasker {

    private static final String MASK = "***";

    private LgpdMasker() {
    }

    /**
     * CPF (11 dígitos) → {@code ***.456.789-**}; CNPJ (14 dígitos) → {@code **.345.678/****-**};
     * demais documentos → {@code ***} + 4 últimos caracteres.
     */
    public static String maskDocument(String document) {
        if (document == null || document.isBlank()) {
            return document;
        }
        String compact = document.replaceAll("[./\\-\\s]", "");
        boolean onlyDigits = compact.chars().allMatch(Character::isDigit);
        if (onlyDigits && compact.length() == 11) {
            return "***." + compact.substring(3, 6) + "." + compact.substring(6, 9) + "-**";
        }
        if (onlyDigits && compact.length() == 14) {
            return "**." + compact.substring(2, 5) + "." + compact.substring(5, 8) + "/****-**";
        }
        String trimmed = document.trim();
        if (trimmed.length() <= 4) {
            return MASK;
        }
        return MASK + trimmed.substring(trimmed.length() - 4);
    }

    /** "Maria Aparecida da Silva" → "Maria A. D. S." */
    public static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        }
        return parts[0] + " " + Arrays.stream(parts, 1, parts.length)
                .map(part -> Character.toUpperCase(part.charAt(0)) + ".")
                .collect(Collectors.joining(" "));
    }

    /** Mantém apenas os 4 últimos dígitos: "(45) 99999-1234" → "(**) *****-1234". */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() <= 4) {
            return MASK;
        }
        return "(**) *****-" + digits.substring(digits.length() - 4);
    }

    /** Oculta logradouro/número, mantendo apenas cidade/UF: "*** - Medianeira/PR". */
    public static String maskAddress(String city, String state) {
        StringBuilder sb = new StringBuilder(MASK);
        boolean hasCity = city != null && !city.isBlank();
        boolean hasState = state != null && !state.isBlank();
        if (hasCity || hasState) {
            sb.append(" - ");
            if (hasCity) {
                sb.append(city.trim());
            }
            if (hasCity && hasState) {
                sb.append("/");
            }
            if (hasState) {
                sb.append(state.trim());
            }
        }
        return sb.toString();
    }
}
