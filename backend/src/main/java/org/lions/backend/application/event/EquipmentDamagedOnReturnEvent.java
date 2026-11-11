package org.lions.backend.application.event;

/** Devolução com avaria — Manutenção abre corretiva automaticamente (RN24). */
public record EquipmentDamagedOnReturnEvent(Long equipmentId, Long loanId, Long beneficiaryId, String description) {
}
