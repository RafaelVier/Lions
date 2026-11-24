package org.lions.backend.application.service.report;

import org.lions.backend.domain.exception.BusinessException;

import java.time.LocalDate;

/**
 * Período de consulta (RN45/UC40). Sem datas informadas = últimos {@code defaultDays} dias até hoje; só a data final
 * = os {@code defaultDays} dias que terminam nela; só a inicial = da inicial até hoje.
 */
public record ReportPeriod(LocalDate start, LocalDate end) {

    public static ReportPeriod resolve(LocalDate start, LocalDate end, LocalDate today, int defaultDays) {
        LocalDate effectiveEnd = end != null ? end : today;
        LocalDate effectiveStart = start != null ? start : effectiveEnd.minusDays(Math.max(defaultDays, 1) - 1L);
        if (effectiveStart.isAfter(effectiveEnd)) {
            throw new BusinessException("A data inicial do período deve ser anterior ou igual à data final.");
        }
        return new ReportPeriod(effectiveStart, effectiveEnd);
    }

    public boolean contains(LocalDate date) {
        return date != null && !date.isBefore(start) && !date.isAfter(end);
    }
}
