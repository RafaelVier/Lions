package org.lions.backend.application.service.loan;

import org.lions.backend.application.dto.request.LoanCreateRequest;
import org.lions.backend.application.dto.response.LoanResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Contrato §6.2 — dono: Empréstimos (Frente D). Assinatura fixa; donos podem acrescentar métodos. */
@Service
public class LoanService {

    /** Registra empréstimo (RN17–RN22, RN28): valida beneficiário/equipamento, gera termo e marca EMPRESTADO. */
    @Transactional
    public LoanResponse registerLoan(LoanCreateRequest req) {
        throw new UnsupportedOperationException("Implementado na Frente D");
    }
}
