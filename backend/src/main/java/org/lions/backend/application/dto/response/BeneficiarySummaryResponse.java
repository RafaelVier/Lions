package org.lions.backend.application.dto.response;

import org.lions.backend.domain.entity.Beneficiary;

import java.time.LocalDateTime;

/** Item de listagem de beneficiários — CPF sempre mascarado (LGPD). */
public record BeneficiarySummaryResponse(
        Long id,
        String fullName,
        String maskedCpf,
        String phone,
        String city,
        String state,
        boolean active,
        boolean anonymized,
        LocalDateTime createdAt) {

    public static BeneficiarySummaryResponse from(Beneficiary b) {
        return new BeneficiarySummaryResponse(b.getId(), b.getFullName(), b.getMaskedCpf(),
                b.getPhone(), b.getCity(), b.getState(), b.isActive(), b.isAnonymized(), b.getCreatedAt());
    }
}
