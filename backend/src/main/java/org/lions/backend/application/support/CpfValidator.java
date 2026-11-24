package org.lions.backend.application.support;

/**
 * Validação de CPF pelo algoritmo dos dígitos verificadores (RN05).
 * Utilitário puro (sem Spring) — pode ser usado pelo domínio.
 */
public final class CpfValidator {

    private CpfValidator() {
    }

    /** Mantém apenas os dígitos do documento (retorna {@code null} se a entrada for nula). */
    public static String normalize(String document) {
        if (document == null) {
            return null;
        }
        return document.replaceAll("\\D", "");
    }

    public static boolean isValid(String cpf) {
        String digits = normalize(cpf);
        if (digits == null || digits.length() != 11 || digits.chars().distinct().count() == 1) {
            return false;
        }
        int first = checkDigit(digits, 9);
        int second = checkDigit(digits, 10);
        return first == Character.getNumericValue(digits.charAt(9))
                && second == Character.getNumericValue(digits.charAt(10));
    }

    private static int checkDigit(String digits, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * (length + 1 - i);
        }
        int remainder = (sum * 10) % 11;
        return remainder == 10 ? 0 : remainder;
    }
}
