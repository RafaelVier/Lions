package org.lions.backend.domain.exception;

/** Falha ao consultar um serviço externo (ex.: ViaCEP) — HTTP 503. */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
