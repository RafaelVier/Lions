package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.lions.backend.domain.enums.NotificationChannel;

import java.time.LocalDate;
import java.util.Set;

/**
 * Cadastro/edição de beneficiário (RN04–RN07). Usado em {@code POST/PUT /beneficiarios} e aninhado no pedido público.
 * <ul>
 *   <li>{@code cpf} é obrigatório e validado pelos dígitos verificadores (aceita com ou sem máscara) — RN04;</li>
 *   <li>menor de 18 anos ou {@code legallyIncapable=true} exige {@code legalGuardianName} e {@code legalGuardianCpf}
 *   (RN06);</li>
 *   <li>{@code consentGranted} deve ser {@code true} no cadastro (RN07); na edição os campos de consentimento são
 *   ignorados (use {@code PUT /beneficiarios/{id}/consentimento}).</li>
 * </ul>
 */
public record BeneficiaryRequest(
        @NotBlank(message = "O nome completo é obrigatório.")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
        String fullName,
        @NotBlank(message = "O CPF é obrigatório.")
        @Pattern(regexp = "\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}", message = "CPF inválido (use 000.000.000-00).")
        String cpf,
        @NotNull(message = "A data de nascimento é obrigatória.")
        @Past(message = "A data de nascimento deve estar no passado.")
        LocalDate birthDate,
        @NotBlank(message = "O telefone (WhatsApp) é obrigatório.")
        @Size(max = 25, message = "O telefone deve ter no máximo 25 caracteres.")
        String phone,
        @Email(message = "E-mail inválido.")
        @Size(max = 120, message = "O e-mail deve ter no máximo 120 caracteres.")
        String email,
        @NotBlank(message = "O CEP é obrigatório.")
        @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP inválido (use 00000-000).")
        String cep,
        @NotBlank(message = "O logradouro é obrigatório.")
        @Size(max = 150, message = "O logradouro deve ter no máximo 150 caracteres.")
        String street,
        @NotBlank(message = "O número é obrigatório.")
        @Size(max = 20, message = "O número deve ter no máximo 20 caracteres.")
        String number,
        @Size(max = 100, message = "O complemento deve ter no máximo 100 caracteres.")
        String complement,
        @NotBlank(message = "O bairro é obrigatório.")
        @Size(max = 80, message = "O bairro deve ter no máximo 80 caracteres.")
        String neighborhood,
        @NotBlank(message = "A cidade é obrigatória.")
        @Size(max = 80, message = "A cidade deve ter no máximo 80 caracteres.")
        String city,
        @NotBlank(message = "A UF é obrigatória.")
        @Pattern(regexp = "[A-Za-z]{2}", message = "UF inválida (2 letras).")
        String state,
        @NotBlank(message = "O nome do contato alternativo é obrigatório.")
        @Size(max = 150, message = "O nome do contato alternativo deve ter no máximo 150 caracteres.")
        String alternativeContactName,
        @NotBlank(message = "O telefone do contato alternativo é obrigatório.")
        @Size(max = 25, message = "O telefone do contato alternativo deve ter no máximo 25 caracteres.")
        String alternativeContactPhone,
        @Size(max = 60, message = "O parentesco deve ter no máximo 60 caracteres.")
        String alternativeContactRelationship,
        Boolean legallyIncapable,
        @Size(max = 150, message = "O nome do responsável deve ter no máximo 150 caracteres.")
        String legalGuardianName,
        @Pattern(regexp = "\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}", message = "CPF do responsável inválido (use 000.000.000-00).")
        String legalGuardianCpf,
        @Size(max = 25, message = "O telefone do responsável deve ter no máximo 25 caracteres.")
        String legalGuardianPhone,
        @Email(message = "E-mail do responsável inválido.")
        @Size(max = 120, message = "O e-mail do responsável deve ter no máximo 120 caracteres.")
        String legalGuardianEmail,
        @Size(max = 60, message = "O parentesco do responsável deve ter no máximo 60 caracteres.")
        String legalGuardianRelationship,
        @Size(max = 2000, message = "As observações devem ter no máximo 2000 caracteres.")
        String notes,
        Boolean consentGranted,
        @Size(max = 150, message = "O nome de quem consentiu deve ter no máximo 150 caracteres.")
        String consentGivenBy,
        Set<NotificationChannel> notificationChannels) {
}
