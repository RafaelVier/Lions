package org.lions.backend.application.event;

import org.lions.backend.domain.enums.EquipmentType;

/** Equipamento ficou disponível (criação, manutenção APTO, devolução BOM_ESTADO, cancelamento, liberação de reserva). */
public record EquipmentAvailableEvent(Long equipmentId, EquipmentType type) {
}
