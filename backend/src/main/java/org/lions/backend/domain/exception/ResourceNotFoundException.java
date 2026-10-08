package org.lions.backend.domain.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Object id) {
        super(String.format("%s não encontrado(a) com identificador: %s", resourceName, id));
    }
}
