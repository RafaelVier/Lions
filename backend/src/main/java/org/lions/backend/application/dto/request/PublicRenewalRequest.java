package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Renovação pelo próprio beneficiário, sem login (UC16): identifica o empréstimo pelo CPF e data de nascimento do
 * beneficiário e pelo número de patrimônio do equipamento (etiqueta/QR Code).
 */
public record PublicRenewalRequest(
        @NotBlank(message = "O CPF é obrigatório.")
        @Pattern(regexp = "\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}", message = "CPF inválido (use 000.000.000-00).")
        String cpf,
        @NotNull(message = "A data de nascimento é obrigatória.")
        LocalDate birthDate,
        @NotBlank(message = "O número de patrimônio é obrigatório.")
        @Size(max = 40, message = "Número de patrimônio inválido.")
        String patrimonyNumber) {
}
