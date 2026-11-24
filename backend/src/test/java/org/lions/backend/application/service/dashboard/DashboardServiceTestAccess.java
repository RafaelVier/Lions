package org.lions.backend.application.service.dashboard;

import java.math.BigDecimal;

/** Expõe métodos de pacote do {@link DashboardService} para testes de outros pacotes. */
public final class DashboardServiceTestAccess {

    private DashboardServiceTestAccess() {
    }

    public static BigDecimal rate(long part, long total) {
        return DashboardService.rate(part, total);
    }
}
