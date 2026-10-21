# 🦁 OrtoEmpresta — Guia Completo da Arquitetura e Status do Backend
> **Documento Oficial de Alinhamento Técnico da Equipe 6**  
> **Disciplina:** Oficina de Gestão e Desenvolvimento de Sistemas Computacionais (8º Período — Bacharelado em Ciência da Computação)  
> **Cliente / Parceiro:** Lions Clube de Medianeira — Distrito LD-1  
> **Status:** Estrutura Base de Domínio, Persistência, Segurança e Serviços Concluída (Pronto para implementação dos Controllers, Testes e Integrações)

---

## 👥 1. Identificação da Equipe e Papéis

| Integrante | RA | Papel Sugerido / Área |
|---|---|---|
| **Henrique Triches** | 2501333 | Backend (Arquitetura DDD, Segurança, Modelagem) |
| **Rodrigo Caio Koelln** | 2624940 | Backend / Integrações & Controllers REST |
| **Gabriela Kolben** | 2485877 | Frontend (React PWA) / UI & Acessibilidade |
| **Fernando Camiran** | 2553686 | Backend / Testes Automatizados & Migrations Flyway |
| **Kauan Lopes** | 2400642 | Frontend (React PWA) / Formulário em 3 Etapas |
| **Kaique Patricio** | 2301520 | Infraestrutura (Docker, Nginx, VPS) & QA |

---

## 🎯 2. Visão Geral do Projeto e Problema Resolvido

O **Lions Clube de Medianeira** gerencia um **banco ortopédico** com acervo estimado em 150 equipamentos (cadeiras de rodas, cadeiras de banho, andadores e muletas).

### 🚨 O Processo Atual (AS-IS) e seus Gargalos:
1. **Centralização extrema:** Todo o atendimento é feito pelo presidente do clube (Márcio) no WhatsApp pessoal.
2. **Controle frágil:** Feito em uma planilha do Excel, sem histórico confiável de manutenções ou devoluções.
3. **Cobrança manual de 90 dias:** Márcio precisa consultar a planilha e cobrar contato por contato manualmente.
4. **Falta de respaldo jurídico:** Não há termo de responsabilidade assinado formalmente nem registro fotográfico das condições de entrega e devolução.

### 💡 A Solução Proposta (TO-BE) — OrtoEmpresta:
- **PWA Mobile-First:** Funciona no navegador do celular, sem necessidade de loja de aplicativos.
- **Área do Solicitante:** Pedido público simplificado em 3 etapas (com preenchimento automático de endereço por CEP).
- **Área do Administrador:** Painel com indicadores rápidos, fila de pedidos, controle de estoque e botão **"Lembrar no WhatsApp" (com mensagem pronta em 1 toque)**.
- **Conformidade com a LGPD:** Tratamento do motivo do pedido como dado sensível de saúde (Art. 5º, II e Art. 11, I da LGPD) com consentimento explícito e mascaramento de documentos nas listagens.
- **Termo de Responsabilidade Formal:** Geração de PDF com hash criptográfico SHA-256 e registro de aceite eletrônico (data, hora e identificação).

---

## 🛠️ 3. Stack Tecnológica Implementada

| Tecnologia | Versão | Função no Projeto |
|---|---|---|
| **Java** | 21 (LTS) | Linguagem base moderna com record patterns e virtual threads |
| **Spring Boot** | 3.3.4 | Framework principal da aplicação |
| **Spring Security** | 6.x | Controle de autenticação e autorização por roles |
| **JJWT (Java JWT)** | 0.12.6 | Geração e validação de tokens JWT Stateless |
| **Spring Data JPA** | 3.3.x | Abstração de persistência com Hibernate ORM |
| **H2 Database** | Runtime | Banco de dados relacional em memória para desenvolvimento rápido |
| **PostgreSQL** | 16 | Banco de dados relacional de produção |
| **Flyway** | 10.x | Versionamento e migração de schema do banco de dados |
| **Spring Cloud OpenFeign**| 2023.0.3 | Cliente declarativo HTTP para integração com o ViaCEP |
| **Springdoc OpenAPI** | 2.6.0 | Documentação interativa Swagger com suporte a Bearer Token |
| **OpenPDF** | 2.0.3 | Geração em tempo de execução do Termo de Responsabilidade em PDF |
| **Lombok** | 1.18.x | Redução de código boilerplate (Getters, Setters, Builders) |

---

## 🏛️ 4. Arquitetura DDD (Domain-Driven Design)

O backend foi integralmente construído sob os princípios do **Domain-Driven Design (DDD)** e da **Arquitetura Hexagonal (Ports & Adapters)**. O objetivo é que **a regra de negócio não dependa do banco de dados ou de frameworks externos**.

```
backend/src/main/java/org/lions/backend/
├── LionsBackendApplication.java       <-- Ponto de entrada Spring Boot (@EnableFeignClients, @EnableJpaAuditing)
│
├── domain/                            <-- CAMADA DE DOMÍNIO (Núcleo Puro de Negócio)
│   ├── entity/                        <-- Entidades puras sem anotações JPA
│   │   ├── Equipment.java             <-- Regras de conservação, baixa e disponibilidade
│   │   ├── Requester.java             <-- Dados do solicitante/beneficiário, validações e máscara LGPD
│   │   ├── LoanOrder.java             <-- Pedido de empréstimo (aprovação, rejeição, pendência)
│   │   ├── Loan.java                  <-- Empréstimo, regra dos 90 dias, renovação sucessiva
│   │   ├── LoanReturn.java            <-- Devolução, identificação de avaria e encaminhamento
│   │   ├── Maintenance.java           <-- Histórico de manutenção e custo de reparo
│   │   ├── User.java                  <-- Usuários do sistema (Márcio / Voluntários)
│   │   ├── LionsClub.java             <-- Dados institucionais do clube
│   │   └── Member.java / SocialAction <-- Dados complementares do clube
│   ├── enums/                         <-- Enumerações de domínio
│   │   ├── EquipmentType.java         <-- CADEIRA_DE_RODAS, CADEIRA_DE_BANHO, ANDADOR, MULETAS
│   │   ├── EquipmentCondition.java    <-- NOVO, SEMINOVO, USADO, EM_MANUTENCAO, BAIXADO
│   │   ├── EquipmentStatus.java       <-- DISPONIVEL, EMPRESTADO, EM_MANUTENCAO, BAIXADO
│   │   ├── OrderStatus.java           <-- PENDENTE_ANALISE, APROVADO, REJEITADO, PENDENTE_ESTOQUE
│   │   ├── LoanStatus.java            <-- ATIVO, RENOVADO, DEVOLVIDO, ATRASADO
│   │   ├── ReturnCondition.java       <-- BOM_ESTADO, PRECISA_MANUTENCAO, DANIFICADO
│   │   ├── MaintenanceStatus.java     <-- EM_ANDAMENTO, CONCLUIDA, IRRECUPERAVEL
│   │   ├── DocumentType.java          <-- CPF, ESTRANGEIRO, CNPJ
│   │   └── UserRole.java              <-- ROLE_ADMIN, ROLE_MEMBER
│   ├── repository/                    <-- PORTS (Interfaces abstratas que o domínio exige)
│   │   ├── EquipmentRepositoryPort.java
│   │   ├── RequesterRepositoryPort.java
│   │   ├── LoanOrderRepositoryPort.java
│   │   ├── LoanRepositoryPort.java
│   │   ├── LoanReturnRepositoryPort.java
│   │   ├── MaintenanceRepositoryPort.java
│   │   └── UserRepositoryPort.java
│   └── exception/                     <-- Exceções semânticas de negócio
│       ├── BusinessException.java
│       ├── ResourceNotFoundException.java
│       └── UnauthorizedException.java
│
├── application/                       <-- CAMADA DE APLICAÇÃO (Casos de Uso e Orquestração)
│   ├── dto/
│   │   ├── request/                   <-- Objetos de entrada com Bean Validation (@NotBlank, @NotNull)
│   │   │   ├── LoginRequest.java
│   │   │   ├── RegisterRequest.java
│   │   │   ├── EquipmentCreateRequest.java
│   │   │   ├── EquipmentUpdateRequest.java
│   │   │   ├── LoanOrderCreateRequest.java   <-- Formulário de 3 etapas do wireframe
│   │   │   ├── LoanApprovalRequest.java      <-- Vinculação por número de patrimônio
│   │   │   ├── LoanReturnRequest.java        <-- Condição no retorno e fotos
│   │   │   ├── MaintenanceRequest.java
│   │   │   └── TermAcceptanceRequest.java    <-- Aceite eletrônico
│   │   └── response/                  <-- Objetos de saída seguros para a API
│   │       ├── AuthResponse.java             <-- Token JWT e perfil
│   │       ├── EquipmentResponse.java
│   │       ├── LoanOrderResponse.java        <-- Exibe documento mascarado (LGPD)
│   │       ├── LoanResponse.java             <-- Inclui link wa.me pronto para envio
│   │       ├── LoanReturnResponse.java
│   │       ├── MaintenanceResponse.java
│   │       └── DashboardIndicatorsResponse.java <-- Dados do painel (38 disp, 104 emp, etc.)
│   └── service/                       <-- Casos de uso / Serviços de aplicação
│       ├── AuthService.java                  <-- Autenticação e auto-criação do admin inicial
│       ├── EquipmentService.java            <-- Gestão do acervo e carga inicial (seed)
│       ├── LoanOrderService.java            <-- Criação, análise e aprovação de pedidos
│       ├── PdfTermService.java              <-- Geração do PDF com OpenPDF + hash SHA-256
│       └── WhatsAppMessageHelper.java       <-- Gerador de links wa.me personalizados
│
└── infrastructure/                    <-- CAMADA DE INFRAESTRUTURA (Adaptadores e Frameworks)
    ├── client/                        <-- Spring Cloud Feign
    │   ├── ViaCepClient.java          <-- Integração com api.viacep.com.br
    │   └── CepResponse.java           <-- DTO de resposta do ViaCEP
    ├── config/                        <-- Configurações gerais
    │   └── OpenApiConfig.java         <-- Swagger UI com suporte a Bearer JWT
    ├── security/                      <-- Segurança Spring Security 6
    │   ├── SecurityConfig.java        <-- Rotas abertas (Swagger, pedidos, login) e fechadas
    │   ├── JwtTokenProvider.java      <-- Geração e validação criptográfica do token
    │   ├── JwtAuthenticationFilter.java <-- Filtro interceptor OncePerRequest
    │   ├── CustomUserDetails.java     <-- Implementação de UserDetails do Spring
    │   └── UserDetailsServiceImpl.java<-- Busca de usuário na port UserRepositoryPort
    └── persistence/                   <-- Persistência com Spring Data JPA
        ├── entity/                    <-- Entidades com @Entity, @Table, @Column
        │   ├── EquipmentJpaEntity.java
        │   ├── RequesterJpaEntity.java
        │   ├── LoanOrderJpaEntity.java
        │   ├── LoanJpaEntity.java
        │   ├── LoanReturnJpaEntity.java
        │   ├── MaintenanceJpaEntity.java
        │   └── UserJpaEntity.java
        ├── repository/                <-- Interfaces JpaRepository
        │   ├── EquipmentJpaRepository.java
        │   ├── RequesterJpaRepository.java
        │   ├── LoanOrderJpaRepository.java
        │   ├── LoanJpaRepository.java
        │   ├── LoanReturnJpaRepository.java
        │   ├── MaintenanceJpaRepository.java
        │   └── UserJpaRepository.java
        └── adapter/                   <-- Adaptadores que implementam as Ports do domínio
            ├── EquipmentRepositoryAdapter.java
            ├── RequesterRepositoryAdapter.java
            ├── LoanOrderRepositoryAdapter.java
            ├── LoanRepositoryAdapter.java
            ├── LoanReturnRepositoryAdapter.java
            ├── MaintenanceRepositoryAdapter.java
            └── UserRepositoryAdapter.java
```

---

## 🔒 5. Segurança e Regras de Privacidade (LGPD)

### Autenticação e Permissões (Spring Security)
1. **Padrão Stateless:** A API não guarda sessão em memória; utiliza tokens JWT assinados com HMAC SHA-256 no header:
   ```http
   Authorization: Bearer <TOKEN_JWT>
   ```
2. **Rotas Públicas (Sem necessidade de autenticação):**
   - `POST /api/v1/auth/login` (Login do Márcio / voluntários)
   - `POST /api/v1/pedidos` (Envio do formulário de pedido pelo solicitante)
   - `GET /api/v1/pedidos/{id}/termo` (Download do termo para leitura)
   - `POST /api/v1/pedidos/{id}/aceite-termo` (Aceite formal pelo beneficiário)
   - `GET /api/v1/cep/{cep}` (Consulta de CEP via ViaCEP para preenchimento de endereço)
   - `/swagger-ui/**`, `/v3/api-docs/**` (Documentação Swagger)
   - `/h2-console/**` (Acesso ao banco de dados de desenvolvimento)
3. **Rotas Protegidas (`ROLE_ADMIN` / `ROLE_MEMBER`):**
   - Gestão de equipamentos, aprovação/rejeição de pedidos, devoluções, manutenções e painel de indicadores.
4. **Usuário Administrador Pré-cadastrado:**
   - **Login:** `marcio`
   - **Senha:** `lions123`
   - *(Criado automaticamente na inicialização via `AuthService.initDefaultAdmin()` caso a tabela esteja vazia).*

### Tratamento de Dados Pessoais e Sensíveis (LGPD)
- **Dados Sensíveis de Saúde:** O campo `needReason` (ex: "alta hospitalar após cirurgia no fêmur") é catalogado como dado sensível (Art. 5º, II da LGPD). O envio só é permitido mediante confirmação explícita de `consentGranted = true` (Art. 11, I da LGPD).
- **Minimização e Mascaramento:** O método `Requester.getMaskedDocument()` oculta os dígitos centrais de CPFs (`***.456.789-**`) e CNPJs nas listagens gerais.

---

## 📄 6. Funcionalidades do Documento da Equipe Já Modeladas

| Requisito do Documento | Status no Backend | Onde foi implementado |
|---|---|---|
| **RF01:** Pedido pelo solicitante (CPF, Estrangeiro, CNPJ) | ✅ Concluído | `LoanOrderCreateRequest`, `Requester.java`, `LoanOrderService` |
| **RF02:** Cadastro assistido pelo Márcio | ✅ Concluído | Flag `assistedRegistration` em `Requester` e `LoanOrder` |
| **RF03:** Cadastro de equipamentos por patrimônio e condição | ✅ Concluído | `EquipmentService`, `EquipmentCreateRequest`, `Equipment` |
| **RF04:** Análise e aprovação de pedidos com lista pendente | ✅ Concluído | `LoanOrderService.approveOrder()`, `rejectOrder()`, `markPendingStock()` |
| **RF05:** Empréstimo com termo em PDF e aceite eletrônico | ✅ Concluído | `PdfTermService` (OpenPDF + SHA-256), `Loan.java` |
| **RF06:** Entrega e devolução com fotos e condições | ✅ Concluído | `LoanReturnRequest`, `LoanReturnService`, `LoanReturn.java` |
| **RF07:** Registro de manutenção por equipamento | ✅ Concluído | `Maintenance.java`, `MaintenanceService` |
| **RF08:** Controle de prazos de 90 dias e lembretes WhatsApp | ✅ Concluído | `Loan.renew()`, `Loan.isDueSoon()`, `WhatsAppMessageHelper` |
| **RF09:** Renovação sucessiva sem limite de prazo | ✅ Concluído | `Loan.renew()` incrementa `renewalCount` e soma +90 dias |
| **RF10:** Histórico por beneficiário e equipamento | ✅ Concluído | `LoanJpaRepository.findByRequesterId()` e `findByEquipmentId()` |
| **RF11:** Painel de indicadores do Márcio (Wireframe 2) | ✅ Concluído | `DashboardIndicatorsResponse`, contadores em `EquipmentService` |
| **RF12:** Login do administrador | ✅ Concluído | `AuthService.login()`, `JwtTokenProvider`, `SecurityConfig` |

---

## 🚀 7. O que a Equipe Precisa Fazer a Partir de Agora

Para os colegas que estão entrando no projeto, aqui está o backlog claro das próximas etapas a serem desenvolvidas:

### 📌 1. Implementar a Camada de Apresentação (Controllers REST)
*Criar no pacote `org.lions.backend.presentation.controller`:*
- `AuthController`: expor `/api/v1/auth/login` e `/api/v1/auth/register`.
- `EquipmentController`: expor `GET`, `POST`, `PUT`, `DELETE` de `/api/v1/equipamentos`.
- `LoanOrderController`: expor `POST /api/v1/pedidos`, `GET /api/v1/pedidos`, `POST /api/v1/pedidos/{id}/aprovar`, `POST /api/v1/pedidos/{id}/rejeitar`.
- `LoanController`: expor devolução (`POST /api/v1/emprestimos/{id}/devolucao`), renovação (`POST /api/v1/emprestimos/{id}/renovar`) e download do PDF do termo.
- `DashboardController`: expor `GET /api/v1/dashboard` trazendo os contadores e os empréstimos a vencer/vencidos com os links de WhatsApp já montados.
- `ViaCepController`: expor `GET /api/v1/cep/{cep}` chamando o `ViaCepClient`.
- `GlobalExceptionHandler` (em `presentation.handler`): capturar `BusinessException` e `ResourceNotFoundException` retornando HTTP 400 e 404 padronizados.

### 📌 2. Migrations do Flyway (Banco de Dados PostgreSQL)
*Criar o arquivo `src/main/resources/db/migration/V1__init_schema.sql`:*
- Tabelas: `tb_users`, `tb_equipments`, `tb_requesters`, `tb_loan_orders`, `tb_loans`, `tb_loan_returns`, `tb_maintenances`.
- Carga inicial (`seed`) de dados com os equipamentos padrão e o usuário administrador.

### 📌 3. Upload e Armazenamento de Fotos (Entrega / Devolução)
- Implementar o recebimento de `MultipartFile` para as fotos das avarias e dos equipamentos na entrega/devolução (salvando em volume local do Docker ou bucket).

### 📌 4. Rotina Automática Agendada (`@Scheduled`)
- Criar um componente `@Component` com `@Scheduled(cron = "0 0 1 * * ?")` (execução diária à 01h da manhã) que busca empréstimos vencidos e chama `loan.markAsOverdue()`.

### 📌 5. Testes Unitários e de Integração
- Criar testes com JUnit 5 e Mockito para `EquipmentServiceTest`, `LoanServiceTest` e `LoanOrderServiceTest`.

---

## 💻 8. Como Executar e Testar Localmente

### 1. Clonar e Acessar o Repositório:
```bash
git clone -b backend https://github.com/RafaelVier/Lions.git
cd Lions/backend
```

### 2. Rodar a Aplicação:
```bash
mvn spring-boot:run
```
*(A aplicação subirá na porta 8080 utilizando o perfil `dev` com banco H2 em memória já pré-carregado).*

### 3. Acessar a Documentação e Testar Endpoints:
- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Console do Banco H2:** [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  - JDBC URL: `jdbc:h2:mem:lionsdb`
  - User: `sa`
  - Password: *(deixar em branco)*
- **Credenciais do Administrador:**
  - Usuário: `marcio`
  - Senha: `lions123`

---
*Este documento foi estruturado para fornecer à equipe todo o direcionamento arquitetural, funcional e prático para o desenvolvimento colaborativo do sistema OrtoEmpresta.*
