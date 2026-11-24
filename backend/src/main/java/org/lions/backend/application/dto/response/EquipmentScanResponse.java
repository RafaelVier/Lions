package org.lions.backend.application.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Resultado da leitura do QR Code (RN15): o item, as operações possíveis no status atual e o contexto
 * (empréstimo em aberto, manutenção em andamento ou reserva).
 */
public record EquipmentScanResponse(
        EquipmentResponse equipment,
        List<EquipmentOperation> availableOperations,
        Long currentLoanId,
        Long currentBeneficiaryId,
        String currentBeneficiaryName,
        LocalDate expectedReturnDate,
        Long openMaintenanceId,
        Long reservedForBeneficiaryId,
        String reservedForBeneficiaryName) {
}
