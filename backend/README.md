# 🦁 OrtoEmpresta — Backend (Spring Boot 3 + Java 21 + Gradle)

Módulo backend do **OrtoEmpresta** (Banco Ortopédico do Lions Clube de Medianeira — Equipe 6).

> 📘 Arquitetura, perfis, regras, rotinas agendadas e variáveis de ambiente:
> [**docs/DESCRICAO_DO_PROJETO_E_BACKEND.md**](../docs/DESCRICAO_DO_PROJETO_E_BACKEND.md).
> Contrato completo da API: [`docs/specs/001-alinhamento-planilha/andamento.md`, seção 6](../docs/specs/001-alinhamento-planilha/andamento.md).

## ⚡ Como rodar

Pré-requisito: **JDK 21**. O Gradle vem pelo wrapper (`./gradlew`; no Windows, `gradlew.bat`).

```bash
cd backend
./gradlew bootRun     # perfil dev, H2 em memória
./gradlew test        # suíte de testes (H2)
./gradlew build       # compila, testa e gera o jar
```

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Console H2 (dev):** http://localhost:8080/h2-console (JDBC `jdbc:h2:mem:lionsdb`, usuário `sa`, senha vazia)
- **Administrador inicial:** `marcio` / `Lions@123` (configurável por `LIONS_ADMIN_USERNAME` / `LIONS_ADMIN_PASSWORD`)
- **Perfis de acesso:** `ROLE_ADMIN`, `ROLE_OPERATOR`, `ROLE_VIEWER`

## 🐘 Produção (PostgreSQL + Flyway)

```bash
./gradlew bootJar
TZ=America/Sao_Paulo SPRING_PROFILES_ACTIVE=prod \
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/<banco> \
SPRING_DATASOURCE_USERNAME=<usuario> SPRING_DATASOURCE_PASSWORD=<senha> \
LIONS_JWT_SECRET=<base64 de 256 bits ou mais> LIONS_ADMIN_PASSWORD=<senha forte> \
java -jar build/libs/ortoempresta-backend-0.0.1-SNAPSHOT.jar
```

O Flyway aplica `src/main/resources/db/migration` e o Hibernate só valida o schema (`ddl-auto: validate`).
Para rodar a suíte de testes contra um PostgreSQL vazio:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/<banco> SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver \
SPRING_DATASOURCE_USERNAME=postgres SPRING_DATASOURCE_PASSWORD= SPRING_JPA_HIBERNATE_DDL_AUTO=validate \
SPRING_FLYWAY_ENABLED=true ./gradlew cleanTest test
```
