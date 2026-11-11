package org.lions.backend.application.service.loan;

import org.lions.backend.application.dto.response.BeneficiaryRestriction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Contrato §6.2 — dono: Empréstimos (Frente D). */
@Service
public class BeneficiaryRestrictionService {

    /** Avalia se o beneficiário pode receber novo empréstimo/entrar na fila (RN20). */
    @Transactional(readOnly = true)
    public BeneficiaryRestriction evaluate(Long beneficiaryId) {
        throw new UnsupportedOperationException("Implementado na Frente D");
    }
}
