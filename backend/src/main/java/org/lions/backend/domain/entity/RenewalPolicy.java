package org.lions.backend.domain.entity;

/**
 * Parâmetros da RN23 (lidos do {@code ParameterService}): máximo de renovações e janela, em dias antes do vencimento,
 * a partir da qual a renovação é permitida.
 */
public record RenewalPolicy(int maxRenewals, int windowDays) {

    public RenewalPolicy {
        if (maxRenewals < 0 || windowDays < 0) {
            throw new IllegalArgumentException("Parâmetros de renovação devem ser não negativos.");
        }
    }
}
