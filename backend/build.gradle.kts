plugins {
    java
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "org.lions"
version = "0.0.1-SNAPSHOT"
description = "OrtoEmpresta - Sistema de Gestão e Empréstimo de Equipamentos de Acessibilidade (Lions Clube Medianeira)"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2023.0.3"

val springdocVersion = "2.6.0"
val jjwtVersion = "0.12.6"
val openpdfVersion = "2.0.3"
val zxingVersion = "3.5.3"

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

dependencies {
    // Spring Boot Web (MVC & REST API)
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Spring Boot Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Spring Data JPA
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Spring Security
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Spring Cloud OpenFeign (Para ViaCEP e integrações)
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // Swagger / OpenAPI UI Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

    // OpenPDF (Geração do Termo de Responsabilidade)
    implementation("com.github.librepdf:openpdf:$openpdfVersion")

    // ZXing (QR Code das etiquetas de patrimônio)
    implementation("com.google.zxing:core:$zxingVersion")
    implementation("com.google.zxing:javase:$zxingVersion")

    // Flyway Migrations
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // JWT (JSON Web Token)
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    // Actuator (Health, Metrics)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Databases: H2 (Dev/Test) & PostgreSQL (Prod)
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}

// Evita gerar o jar "plain" adicional; mantém apenas o jar executável do Spring Boot.
tasks.jar {
    enabled = false
}
