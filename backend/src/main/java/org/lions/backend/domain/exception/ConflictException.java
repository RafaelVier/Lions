package org.lions.backend.domain.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Conflito de estado (HTTP 409), ex.: documento já cadastrado para beneficiário ativo (RN04).
 * Os {@code details} são mesclados como propriedades do {@code ProblemDetail}.
 */
public class ConflictException extends RuntimeException {

    private final Map<String, Object> details;

    public ConflictException(String message) {
        this(message, Map.of());
    }

    public ConflictException(String message, Map<String, Object> details) {
        super(message);
        this.details = details == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
