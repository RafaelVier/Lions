package org.lions.backend.application.support;

import org.lions.backend.domain.exception.BusinessException;

import java.util.ArrayList;
import java.util.List;

/**
 * Política de senha forte (RN57): mínimo 8 caracteres, com letra maiúscula, minúscula, número e caractere especial,
 * sem espaços.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;

    private PasswordPolicy() {
    }

    public static List<String> violations(String password) {
        List<String> violations = new ArrayList<>();
        String value = password == null ? "" : password;
        if (value.length() < MIN_LENGTH) {
            violations.add("A senha deve ter no mínimo " + MIN_LENGTH + " caracteres.");
        }
        if (value.chars().noneMatch(Character::isUpperCase)) {
            violations.add("A senha deve conter ao menos uma letra maiúscula.");
        }
        if (value.chars().noneMatch(Character::isLowerCase)) {
            violations.add("A senha deve conter ao menos uma letra minúscula.");
        }
        if (value.chars().noneMatch(Character::isDigit)) {
            violations.add("A senha deve conter ao menos um número.");
        }
        if (value.chars().allMatch(c -> Character.isLetterOrDigit(c) || Character.isWhitespace(c))) {
            violations.add("A senha deve conter ao menos um caractere especial.");
        }
        if (value.chars().anyMatch(Character::isWhitespace)) {
            violations.add("A senha não pode conter espaços.");
        }
        return violations;
    }

    public static boolean isStrong(String password) {
        return violations(password).isEmpty();
    }

    /** Lança {@link BusinessException} com todas as violações quando a senha não é forte. */
    public static void validate(String password) {
        List<String> violations = violations(password);
        if (!violations.isEmpty()) {
            throw new BusinessException(String.join(" ", violations));
        }
    }
}
