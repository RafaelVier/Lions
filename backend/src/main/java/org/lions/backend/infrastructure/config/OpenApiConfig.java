package org.lions.backend.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("OrtoEmpresta API - Lions Clube de Medianeira")
                        .description("API REST para o Sistema de Gestão e Empréstimo de Equipamentos de Acessibilidade (Banco Ortopédico do Lions Clube de Medianeira - Equipe 6).\n\n" +
                                "Arquitetura: DDD (Domain-Driven Design), Spring Boot 3, Spring Data JPA, Spring Cloud, Spring Security JWT e OpenPDF.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipe 6 - Oficina de Gestão e Desenvolvimento de Sistemas Computacionais (8º Período)")
                                .email("suporte@lionsmedianeira.org.br")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Insira o token JWT gerado no endpoint /api/v1/auth/login")));
    }
}
