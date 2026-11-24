package org.lions.backend.application.dto.response;

import java.time.LocalDate;

/** Resultado da renovação pública (UC16) — sem dados pessoais. */
public record PublicRenewalResponse(
        String patrimonyNumber,
        String equipmentTypeDescription,
        LocalDate previousExpectedReturnDate,
        LocalDate expectedReturnDate,
        int renewalCount,
        int maxRenewals) {
}
