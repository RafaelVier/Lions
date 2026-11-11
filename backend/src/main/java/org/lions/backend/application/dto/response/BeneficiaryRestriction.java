package org.lions.backend.application.dto.response;

import java.util.List;

/**
 * Situação do beneficiário para novos empréstimos/fila (RN20).
 *
 * @param blocked           {@code true} se não pode receber novo empréstimo
 * @param hasOpenLoan       possui empréstimo em aberto
 * @param hasOpenOccurrence possui ocorrência (inadimplência/avaria) não resolvida
 * @param reasons           motivos legíveis do bloqueio
 */
public record BeneficiaryRestriction(
        boolean blocked,
        boolean hasOpenLoan,
        boolean hasOpenOccurrence,
        List<String> reasons) {

    public BeneficiaryRestriction {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
