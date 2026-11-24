package org.lions.backend.application.support;

import java.util.LinkedHashMap;
import java.util.Map;

/** Monta mapas ordenados de detalhes de auditoria aceitando valores nulos (ao contrário de {@link Map#of}). */
public final class AuditDetails {

    private AuditDetails() {
    }

    /** {@code of("campo1", valor1, "campo2", valor2, ...)}; chaves com valor {@code null} são omitidas. */
    public static Map<String, Object> of(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Informe pares chave/valor.");
        }
        Map<String, Object> details = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            if (keyValues[i + 1] != null) {
                details.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
            }
        }
        return details;
    }

    /** Par {@code {old, new}} (aceita nulos). */
    public static Map<String, Object> oldNew(Object oldValue, Object newValue) {
        Map<String, Object> change = new LinkedHashMap<>();
        change.put("old", oldValue);
        change.put("new", newValue);
        return change;
    }

    /** Alteração simples {@code {campo: {old, new}}}. */
    public static Map<String, Object> change(String field, Object oldValue, Object newValue) {
        Map<String, Object> change = new LinkedHashMap<>();
        change.put("old", oldValue);
        change.put("new", newValue);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put(field, change);
        return details;
    }
}
