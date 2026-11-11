package org.lions.backend.application.service.maintenance;

import org.lions.backend.application.dto.response.PreventiveMaintenanceDue;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Contrato §6.2 — dono: Equipamentos + Manutenção (Frente C). */
@Service
public class PreventiveMaintenanceService {

    /** Equipamentos ativos que atingiram meses sem manutenção ou ciclos de empréstimo (RN39). */
    @Transactional(readOnly = true)
    public List<PreventiveMaintenanceDue> findDue() {
        throw new UnsupportedOperationException("Implementado na Frente C");
    }
}
