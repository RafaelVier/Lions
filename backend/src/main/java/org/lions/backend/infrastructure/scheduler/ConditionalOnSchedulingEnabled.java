package org.lions.backend.infrastructure.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ativa o bean (job agendado) somente quando {@code lions.scheduling.enabled=true} (default).
 * Nos testes a propriedade é {@code false}.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnProperty(prefix = "lions.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public @interface ConditionalOnSchedulingEnabled {
}
