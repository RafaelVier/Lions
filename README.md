# Lions
Repositório geral para a disciplina Oficina de Gestão e Desenvolvimento de Sistemas Computacionais do 8º periodo

## Organização diretórios

### backend
API REST e regras de negócio desenvolvidas para a solução **OrtoEmpresta** (Banco Ortopédico do Lions Clube de Medianeira).

#### 🛠️ Tecnologias
- **Linguagem:** Java 21 (LTS)
- **Framework Principal:** Spring Boot 3.3.4
- **Segurança:** Spring Security 6 com autenticação Stateless via JWT (JJWT 0.12.6) e RBAC (Roles)
- **Persistência de Dados:** Spring Data JPA / Hibernate
- **Bancos de Dados:** H2 Database (desenvolvimento em memória) e PostgreSQL 16 (produção)
- **Migrations:** Flyway
- **Integrações Cloud:** Spring Cloud OpenFeign (consulta automática de endereço via ViaCEP)
- **Documentação de API:** Swagger / OpenAPI 3.0 (`springdoc-openapi-starter-webmvc-ui:2.6.0`)
- **Documentos & Jurídico:** OpenPDF (geração do Termo de Responsabilidade com hash SHA-256)
- **Rotinas Agendadas:** Spring `@Scheduled` (verificação diária dos prazos de 90 dias)

#### 🏛️ Arquitetura (Domain-Driven Design - DDD)
A aplicação segue os princípios de separação de responsabilidades do DDD com arquitetura em camadas:

- **`domain` (Núcleo de Negócio Puro):**
  - `entity/`: Entidades ricas (`Equipment`, `Requester`, `LoanOrder`, `Loan`, `LoanReturn`, `Maintenance`, `User`).
  - `enums/`: Tipos (`EquipmentType`, `EquipmentCondition`, `LoanStatus`, `OrderStatus`, `ReturnCondition`, etc.).
  - `repository/`: *Ports* (interfaces abstratas de persistência desacopladas de framework).
  - `exception/`: Exceções de negócio (`BusinessException`, `ResourceNotFoundException`, etc.).

- **`application` (Casos de Uso & Orquestração):**
  - `service/`: Serviços de aplicação (`EquipmentService`, `LoanOrderService`, `LoanService`, `PdfTermService`, `WhatsAppMessageHelper`).
  - `dto/`: Objetos de transferência de dados (`request/` e `response/`) com validações Bean Validation e mascaramento de dados sensíveis (LGPD).

- **`infrastructure` (Adaptadores & Detalhes Técnicos):**
  - `persistence/`: Entidades JPA, interfaces Spring Data e *Adapters* implementando as portas do domínio.
  - `security/`: Provedor e filtro JWT, UserDetailsService e SecurityFilterChain com endpoints públicos e protegidos.
  - `client/`: Cliente Feign para integração com a API pública do ViaCEP.
  - `config/`: Configuração do Swagger OpenAPI com suporte a Bearer Token.

#### ⚙️ Como Executar o Backend Localmente

1. **Pré-requisitos:**
   - Java 21 instalado
   - Maven 3.9+ instalado

2. **Entrar no diretório do backend:**
   ```bash
   cd backend
   ```

3. **Executar a aplicação (Perfil `dev` com banco H2 em memória):**
   ```bash
   mvn spring-boot:run
   ```

4. **Acessar a documentação interativa da API:**
   - **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
   - **OpenAPI JSON:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
   - **Console H2 (Dev):** [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:mem:lionsdb`, Usuário: `sa`, Senha em branco)


### frontend
Projeto frontend

### infra
Definições de infraestrutura e manifestos de configuração

### docs
Documentação da aplicação, artefatos em geral (requisitos funcionais, requisitos não funcionais...)

### db
Artefatos de banco de dados, scripts, migrations...
