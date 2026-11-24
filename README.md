# Lions
Repositório geral para a disciplina Oficina de Gestão e Desenvolvimento de Sistemas Computacionais do 8º periodo

## Organização diretórios

### backend
API REST e regras de negócio desenvolvidas para a solução **OrtoEmpresta** (Banco Ortopédico do Lions Clube de Medianeira).

#### 🛠️ Tecnologias
- **Linguagem:** Java 21 (LTS)
- **Build:** Gradle 8.14 (Kotlin DSL) via Gradle Wrapper
- **Framework:** Spring Boot 3.3.4
- **Segurança:** Spring Security 6, JWT stateless (JJWT 0.12.6, sessão de 30 min) e perfis `ROLE_ADMIN`, `ROLE_OPERATOR` e `ROLE_VIEWER`
- **Persistência:** Spring Data JPA / Hibernate
- **Bancos de dados:** H2 (desenvolvimento e testes) e PostgreSQL 16 (produção)
- **Migrations:** Flyway (`V1__init_schema.sql`, `V2__frente_g_sessoes_sincronizacao.sql`)
- **Integrações:** ViaCEP (OpenFeign), e-mail (SMTP) e SMS/WhatsApp (Twilio), opcionais por configuração
- **Documentação de API:** Swagger / OpenAPI 3 (`springdoc-openapi-starter-webmvc-ui:2.6.0`)
- **Documentos:** OpenPDF (termo de responsabilidade com hash SHA-256 e relatórios) e ZXing (QR Code)
- **Rotinas agendadas:** Spring `@Scheduled` (atrasos, preventivas, reservas da fila, lembretes, resumo diário, sessões)

#### 🏛️ Arquitetura (DDD / hexagonal)
- **`domain`:** entidades com as regras de negócio, enums, *ports* de persistência e exceções.
- **`application`:** casos de uso por módulo (`auth`, `user`, `beneficiary`, `order`, `equipment`, `maintenance`,
  `loan`, `waitlist`, `notification`, `dashboard`, `report`, `help`, `assistant`, `sync`, `audit`, `parameter`),
  eventos entre módulos, DTOs e utilitários (CPF, senha forte, mascaramento LGPD).
- **`infrastructure`:** JPA e adapters, segurança JWT, envio de notificações, rotinas agendadas, QR Code, storage e ViaCEP.
- **`presentation`:** controllers REST em `/api/v1` e tratamento de erros no formato *Problem Details*.

Descrição completa (perfis, regras, variáveis de ambiente, rotinas e rotas):
[`docs/DESCRICAO_DO_PROJETO_E_BACKEND.md`](docs/DESCRICAO_DO_PROJETO_E_BACKEND.md). Contrato da API:
[`docs/specs/001-alinhamento-planilha/andamento.md`](docs/specs/001-alinhamento-planilha/andamento.md) (seção 6).

#### ⚙️ Como executar o backend localmente

1. **Pré-requisitos:** Java 21 (ex.: [SDKMAN](https://sdkman.io/) `sdk install java 21.0.11-tem`). Não é preciso
   instalar o Gradle: o projeto usa o wrapper (`gradlew`).

2. **Desenvolvimento (perfil `dev`, H2 em memória):**
   ```bash
   cd backend
   ./gradlew bootRun
   ```
   No Windows: `gradlew.bat bootRun`. Outros comandos: `./gradlew test` (testes) e `./gradlew build`.

3. **Acessos:**
   - **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
   - **OpenAPI JSON:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
   - **Console H2 (dev):** [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:mem:lionsdb`, usuário `sa`, senha em branco)
   - **Administrador inicial:** `marcio` / `Lions@123` (configurável por `LIONS_ADMIN_USERNAME` e `LIONS_ADMIN_PASSWORD`)

4. **Produção (PostgreSQL + Flyway):** perfil `prod` (`SPRING_PROFILES_ACTIVE=prod`) com `SPRING_DATASOURCE_URL`,
   `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `LIONS_JWT_SECRET` e `TZ=America/Sao_Paulo`.
   Detalhes em [`backend/README.md`](backend/README.md).


### frontend
Projeto frontend

### infra - Definições de infraestrutura e manifestos de configuração
MER: https://lucid.app/lucidchart/4c0faa0a-7257-4d49-aa22-3be76df1b543/edit?viewport_loc=-1354%2C-371%2C1895%2C1003%2C0_0&invitationId=inv_9a4e1d8e-d762-467f-bb5f-3b2ee6f9e779

### DOCS

#### Documentação de negócios:
  - **Rquisitos:** https://docs.google.com/spreadsheets/d/1XApMBVegQe_ZXXPou89hw5Tg45RCIoCAEIXQ-1YhBBc/edit?gid=213998024#gid=213998024
  - **Regras de Negócios:** https://docs.google.com/spreadsheets/d/1XApMBVegQe_ZXXPou89hw5Tg45RCIoCAEIXQ-1YhBBc/edit?gid=726238991#gid=726238991
  - **Requisitos dicididos para trabalhar no momento:** https://docs.google.com/spreadsheets/d/1XApMBVegQe_ZXXPou89hw5Tg45RCIoCAEIXQ-1YhBBc/edit?gid=206774340#gid=206774340
  - **Matrix rastreabilidade:** https://docs.google.com/spreadsheets/d/1XApMBVegQe_ZXXPou89hw5Tg45RCIoCAEIXQ-1YhBBc/edit?gid=998261716#gid=998261716
  - **Casos de uso:** https://docs.google.com/spreadsheets/d/1XApMBVegQe_ZXXPou89hw5Tg45RCIoCAEIXQ-1YhBBc/edit?gid=425242948#gid=425242948
  - **Protótipo front-end:** https://www.figma.com/design/PSJREjqnsyN5riXIEl8i58/Lions-Club?node-id=0-1&t=wMMIEU6MvO0QLVJz-1

### Banco de dados
  - **MER:** https://lucid.app/lucidchart/4c0faa0a-7257-4d49-aa22-3be76df1b543/edit?viewport_loc=-1354%2C-371%2C1895%2C1003%2C0_0&invitationId=inv_9a4e1d8e-d762-467f-bb5f-3b2ee6f9e779
