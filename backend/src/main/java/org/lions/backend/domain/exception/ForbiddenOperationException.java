package org.lions.backend.domain.exception;

/**
 * Operação de negócio não permitida para o perfil do usuário corrente (HTTP 403),
 * ex.: empréstimo retroativo por quem não é Administrador (RN22) ou lançamento de ajuste (RN41).
 */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
