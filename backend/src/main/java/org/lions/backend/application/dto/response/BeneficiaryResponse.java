package org.lions.backend.application.dto.response;

import org.lions.backend.domain.entity.Beneficiary;
import org.lions.backend.domain.enums.NotificationChannel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/** Cadastro completo do beneficiário (detalhe — perfis Admin/Operador). */
public record BeneficiaryResponse(
        Long id,
        String fullName,
        String cpf,
        String maskedCpf,
        LocalDate birthDate,
        Integer age,
        boolean minor,
        boolean requiresLegalGuardian,
        String phone,
        String email,
        String cep,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String alternativeContactName,
        String alternativeContactPhone,
        String alternativeContactRelationship,
        boolean legallyIncapable,
        String legalGuardianName,
        String legalGuardianCpf,
        String legalGuardianPhone,
        String legalGuardianEmail,
        String legalGuardianRelationship,
        boolean consentGranted,
        LocalDateTime consentTimestamp,
        String consentGivenBy,
        Set<NotificationChannel> notificationChannels,
        boolean active,
        LocalDateTime inactivatedAt,
        String inactivationReason,
        boolean anonymized,
        LocalDateTime anonymizedAt,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static BeneficiaryResponse from(Beneficiary b, LocalDate today) {
        Integer age = b.getBirthDate() != null
                ? java.time.Period.between(b.getBirthDate(), today).getYears()
                : null;
        return new BeneficiaryResponse(
                b.getId(), b.getFullName(), b.getCpf(), b.getMaskedCpf(),
                b.getBirthDate(), age, b.isMinor(today), !b.isAnonymized() && b.requiresLegalGuardian(today),
                b.getPhone(), b.getEmail(), b.getCep(), b.getStreet(), b.getNumber(), b.getComplement(),
                b.getNeighborhood(), b.getCity(), b.getState(),
                b.getAlternativeContactName(), b.getAlternativeContactPhone(), b.getAlternativeContactRelationship(),
                b.isLegallyIncapable(), b.getLegalGuardianName(), b.getLegalGuardianCpf(),
                b.getLegalGuardianPhone(), b.getLegalGuardianEmail(), b.getLegalGuardianRelationship(),
                b.isConsentGranted(), b.getConsentTimestamp(), b.getConsentGivenBy(), b.getNotificationChannels(),
                b.isActive(), b.getInactivatedAt(), b.getInactivationReason(), b.isAnonymized(), b.getAnonymizedAt(),
                b.getNotes(), b.getCreatedAt(), b.getUpdatedAt());
    }
}
