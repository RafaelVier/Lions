package org.lions.backend.application.service.waitlist;

import org.lions.backend.application.dto.request.WaitlistEnqueueRequest;
import org.lions.backend.application.dto.response.WaitlistEntryResponse;
import org.lions.backend.domain.enums.EquipmentType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Contrato §6.2 — dono: Fila + Notificações (Frente E). */
@Service
public class WaitlistService {

    /** Inclui o beneficiário na fila do tipo (RN29, RN31, RN34). */
    @Transactional
    public WaitlistEntryResponse enqueue(WaitlistEnqueueRequest req) {
        throw new UnsupportedOperationException("Implementado na Frente E");
    }

    /** Há alguém AGUARDANDO para o tipo (bloqueia renovação — RN23). */
    @Transactional(readOnly = true)
    public boolean hasWaitingFor(EquipmentType type) {
        throw new UnsupportedOperationException("Implementado na Frente E");
    }

    /** O beneficiário tem posição ativa (AGUARDANDO/RESERVADO) em alguma fila. */
    @Transactional(readOnly = true)
    public boolean hasActiveEntry(Long beneficiaryId) {
        throw new UnsupportedOperationException("Implementado na Frente E");
    }

    /** Marca como ATENDIDO a posição do beneficiário para o tipo ao registrar o empréstimo (RN32). */
    @Transactional
    public void markServed(Long beneficiaryId, Long equipmentId, EquipmentType type) {
        throw new UnsupportedOperationException("Implementado na Frente E");
    }
}
