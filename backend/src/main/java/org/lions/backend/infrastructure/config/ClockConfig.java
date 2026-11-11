package org.lions.backend.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /** Relógio único da aplicação; testes substituem por {@code Clock.fixed(...)}. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
