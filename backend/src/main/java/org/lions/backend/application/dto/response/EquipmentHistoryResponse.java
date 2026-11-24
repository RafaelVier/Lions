package org.lions.backend.application.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** Histórico do equipamento: empréstimos, manutenções e custo acumulado (RN38), inclusive de itens baixados (RN55). */
public record EquipmentHistoryResponse(
        EquipmentResponse equipment,
        List<LoanResponse> loans,
        List<MaintenanceResponse> maintenances,
        int loanCount,
        BigDecimal totalMaintenanceCost) {
}
