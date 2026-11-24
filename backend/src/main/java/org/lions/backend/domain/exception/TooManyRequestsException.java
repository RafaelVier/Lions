package org.lions.backend.domain.exception;

/** Limite de tentativas excedido em rota pública — HTTP 429. */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
