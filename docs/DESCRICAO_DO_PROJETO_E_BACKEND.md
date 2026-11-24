# 🦁 OrtoEmpresta — Arquitetura e Estado do Backend
> **Documento de alinhamento técnico da Equipe 6**
> **Disciplina:** Oficina de Gestão e Desenvolvimento de Sistemas Computacionais (8º Período — Bacharelado em Ciência da Computação)
> **Cliente / Parceiro:** Lions Clube de Medianeira — Distrito LD-1
> **Atualizado em:** 24/11/2026 — Spec 001 concluída no backend (Ondas 0, 1 e 2)
>
> **Fonte de verdade das regras:** a planilha de requisitos linkada no `README.md` (abas "Requisitos - Final",
> "Regras de negócio - Sugestões" RN01–RN61, "Casos de Uso" e "Banco de Dados"). Em caso de divergência entre este
> documento e a planilha, **prevalece a planilha**. O detalhamento técnico está em
> [`docs/specs/001-alinhamento-planilha/`](specs/001-alinhamento-planilha/) (`spec.md`, `plan.md`, `tasks.md` e
> `andamento.md`, que traz o **contrato completo da API**).

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

## 🎯 2. Visão Geral do Projeto

O **Lions Clube de Medianeira** gerencia um **banco ortopédico** (cadeiras de rodas, cadeiras de banho, andadores,
muletas e outros itens) emprestados gratuitamente à comunidade.

### Processo atual (AS-IS) e gargalos
1. **Centralização:** todo o atendimento é feito pelo presidente do clube (Márcio) no WhatsApp pessoal.
2. **Controle frágil:** planilha do Excel, sem histórico confiável de manutenções ou devoluções.
3. **Cobrança manual dos prazos:** é preciso consultar a planilha e cobrar contato por contato.
4. **Falta de respaldo jurídico:** não há termo de responsabilidade assinado nem registro das condições de entrega e devolução.

### Solução (TO-BE) — OrtoEmpresta
- **PWA mobile-first** (frontend) consumindo esta API REST, com operação offline sincronizada depois (RN60).
- **Pedido público** sem login (formulário em 3 etapas, endereço por CEP) e **renovação pública** pelo próprio beneficiário (UC16).
- **Área administrativa** com perfis, fila de espera, manutenção, QR Code, dashboard e relatórios.
- **Notificações automáticas** por e-mail, SMS e WhatsApp, com contato manual (link wa.me) só como contingência.
- **LGPD:** consentimento, mascaramento por perfil, anonimização e auditoria.
- **Termo de responsabilidade em PDF** com hash SHA-256 e assinatura digital ou física digitalizada.

---

## 🛠️ 3. Stack

| Tecnologia | Versão | Uso |
|---|---|---|
| **Java** | 21 (LTS) | Linguagem (toolchain do Gradle exige JDK 21) |
| **Gradle** | 8.14 (Kotlin DSL, via wrapper `./gradlew`) | Build e testes |
| **Spring Boot** | 3.3.4 | Web, Validation, Data JPA, Security, Mail, Actuator |
| **JJWT** | 0.12.6 | Tokens JWT HS256 (30 min, com `jti`) |
| **H2** | runtime | Banco em memória (perfis `dev` e testes) |
| **PostgreSQL** | 16 | Banco de produção (perfil `prod`) |
| **Flyway** | 10.x | Migrations `V1__init_schema.sql` e `V2__frente_g_sessoes_sincronizacao.sql` |
| **Spring Cloud OpenFeign** | 2023.0.x | Consulta de CEP no ViaCEP |
| **Springdoc OpenAPI** | 2.6.0 | Swagger UI com Bearer token |
| **OpenPDF** | 2.0.3 | Termo de responsabilidade e relatórios em PDF |
| **ZXing** | 3.5.3 | QR Code das etiquetas (codifica só o patrimônio) |
| **Twilio (REST)** / SMTP | — | Envio de SMS/WhatsApp e e-mail (opcionais, por configuração) |
| **Lombok** | 1.18.x | Redução de boilerplate |

---

## 🏛️ 4. Arquitetura (DDD / hexagonal)

A regra de negócio não depende de banco nem de framework. Os módulos conversam por **serviços de contrato** e por
**eventos de aplicação** (ex.: devolução com avaria → manutenção corretiva; item disponível → reserva para a fila).

```
backend/src/main/java/org/lions/backend/
├── domain/            Núcleo de negócio puro
│   ├── entity/        Beneficiary, Equipment (+ EquipmentSpecification, EquipmentTypeConfig), LoanOrder, Loan,
│   │                  LoanRenewal, LoanReturn, Maintenance (+ MaintenanceCost), WaitlistEntry, Occurrence,
│   │                  Notification, HelpContent, AuditLog, SystemParameter, User, UserSession,
│   │                  SyncOperation, SyncConflict (+ LionsClub, Member, SocialAction, fora da spec)
│   ├── enums/         EquipmentType, EquipmentStatus, LoanStatus, OrderStatus, WaitlistStatus, UserRole, ...
│   ├── repository/    Ports de persistência
│   └── exception/     BusinessException (422), ConflictException (409), ResourceNotFoundException (404), ...
├── application/       Casos de uso
│   ├── service/       auth, user, beneficiary, order, equipment, maintenance, loan, waitlist, notification,
│   │                  dashboard, report, help, assistant, sync, audit, parameter
│   ├── event/         Eventos entre módulos
│   ├── port/          Ports de saída (envio de notificação, QR Code, CEP, storage, usuário atual)
│   ├── dto/           request/ e response/ (Bean Validation)
│   └── support/       Utilitários (CPF, política de senha, mascaramento LGPD, auditoria)
├── infrastructure/    Adaptadores
│   ├── persistence/   Entidades JPA, Spring Data e adapters das ports
│   ├── security/      JWT, filtro, SecurityConfig, sessões persistidas
│   ├── notification/  E-mail (SMTP), SMS e WhatsApp (Twilio), roteamento e fallback manual
│   ├── scheduler/     Rotinas agendadas (seção 8)
│   ├── qrcode/ · storage/ · client/ (ViaCEP) · config/ (admin inicial, tipos, seed dev, Clock, OpenAPI)
└── presentation/
    ├── controller/    20 controllers REST (`/api/v1/...`)
    └── handler/       GlobalExceptionHandler (Problem Details, RFC 7807)
```

---

## 🔒 5. Segurança, perfis e LGPD

### Perfis (RN01)
| Perfil | Role | Pode |
|---|---|---|
| Administrador | `ROLE_ADMIN` | Tudo: usuários, baixa, parâmetros e tipos, cancelamentos, prioridades na fila, encerramento de manutenção, ocorrências, conflitos de sincronização, auditoria |
| Operador/Voluntário | `ROLE_OPERATOR` | Beneficiários, pedidos, empréstimos, assinaturas, devoluções, renovações, manutenções (sem encerrar), fila, notificações |
| Consulta | `ROLE_VIEWER` | Só leitura de dashboard e relatórios, com dados pessoais mascarados (RN43/RN56) |

### Autenticação (RN57)
- JWT no header `Authorization: Bearer <token>`, validade de **30 minutos**. `POST /auth/refresh` renova enquanto houver
  atividade; `POST /auth/logout` revoga. Sessões são persistidas (`tb_user_sessions`): token revogado ou expirado
  recebe 401, e o encerramento por inatividade é auditado (`SESSION_EXPIRED`).
- Senha forte obrigatória: ≥ 8 caracteres com maiúscula, minúscula, dígito e caractere especial.
- Não há cadastro público de usuários: só o Admin cria (`POST /usuarios`). Usuários nunca são apagados, só inativados.
- **Administrador inicial:** criado na subida se ainda não existir — usuário **`marcio`**, senha **`Lions@123`**
  (configuráveis por `LIONS_ADMIN_USERNAME`, `LIONS_ADMIN_PASSWORD`, `LIONS_ADMIN_EMAIL`). **Troque a senha em produção.**

### Rotas públicas (sem token)
`POST /api/v1/auth/login` · `POST /api/v1/pedidos` · `POST /api/v1/emprestimos/renovacao-publica` ·
`GET /api/v1/cep/{cep}` · `GET /api/v1/ajuda/publico` · `GET /api/v1/assistente/perguntas` ·
Swagger (`/swagger-ui.html`, `/v3/api-docs`) · `GET /actuator/health` · `/h2-console` (só existe em `dev`).
O termo **não** é público: download e assinatura exigem Admin/Operador.

### LGPD
- Consentimento obrigatório no cadastro, com data/hora e canais de notificação autorizados (RN07).
- Motivo do pedido tratado como dado sensível de saúde; CPF mascarado nas listagens.
- Perfil Consulta vê nome/CPF/endereço/telefone mascarados nos relatórios e só números no dashboard (RN43/RN56).
- Anonimização pelo Admin preservando o histórico de empréstimos (RN59). Exportações auditadas (RN44).

---

## 📄 6. Requisitos e regras implementados (numeração da planilha)

| RF | Requisito | Principais regras atendidas |
|---|---|---|
| RF01 | Usuários e permissões | RN01–RN03, RN57 (perfis, inativação, senha forte, sessão de 30 min, logout auditado) |
| RF02 | Beneficiários | RN04 só **CPF** (válido, único entre ativos → 409 com `existingId`), RN05 obrigatórios e endereço próprio, RN06 responsável legal para menor/incapaz, RN07 consentimento, RN08 inativação bloqueada com pendências, RN59 anonimização |
| RF03 | Equipamentos | RN09 patrimônio gerado (`PT-0001`), RN11/RN12 status e transições só pelos fluxos, RN13 baixa só Admin com motivo, RN14 tipos com prazo editável, ano e especificações |
| RF04 | QR Code | RN10 QR determinístico só com o patrimônio, RN15 leitura que só devolve as operações possíveis |
| RF05 | Empréstimos e devoluções | RN16 gratuidade, RN17 só item disponível/reservado para o próprio, **RN18 prazo padrão de 30 dias, configurável por tipo**, RN19 máx. 1 por tipo e 2 em aberto, RN20 bloqueio (atraso, termo pendente, ocorrência), RN21 nasce `PENDENTE_ASSINATURA`, RN22 retroativo só Admin, **RN23 máx. 2 renovações**, só a partir de 7 dias antes do vencimento, sem atraso e sem fila para o tipo, RN24/RN40 devolução com avaria → manutenção corretiva + ocorrência `DANO`, RN26 cancelamento só Admin, RN27 atraso e inadimplência com 30 dias, RN28 concorrência (lock + versão) |
| RF06 | Fila de espera | RN29 FIFO por tipo, RN30 prioridade só Admin, RN31 uma posição por tipo, RN32 reserva automática por 48 h, RN33 expiração/recusa (2 seguidas → sai), RN34 inativado ou bloqueado sai da fila |
| RF07 | Termo em PDF | RN06/RN21/RN61 dados do assinante, cláusula de gratuidade, hash SHA-256, sem exclusão |
| RF08 | Manutenções | RN35–RN38, **UC23 encerramento só pelo Admin** (APTO → disponível; INSERVÍVEL → baixa), RN39 preventiva (12 meses ou 10 ciclos), RN41 ajustes |
| RF09/RF10 | Históricos e auditoria | RN41, RN42, RN52 (ações automáticas marcadas), RN58 |
| RF11 | Relatórios PDF/CSV | `INVENTARIO`, `EMPRESTIMOS`, `MANUTENCOES`, `ASSISTIDOS`; CSV UTF-8 com BOM e `;`; RN43–RN45, RN55 |
| RF12 | Notificações | RN46 régua (7 dias, 1 dia, no dia, a cada 3 dias de atraso), aviso de elegível para renovação, RN47 envio automático pelos canais autorizados, RN48 3 tentativas → contato manual, RN49 resumo diário |
| RF13 | Assistente (por regras) | RN50/RN51: sugere alocação fila × itens (só Admin) e responde FAQ; nunca executa |
| RF14 | Ajuda / onboarding | RN53 conteúdo por perfil + FAQ pública |
| RF15 | Dashboard | Ocupação, atrasos, fila por tipo, custos de manutenção e volume de empréstimos; RN54–RN56 |
| RNF06 | Offline | RN60: `POST /sincronizacao` idempotente, conflitos para conferência do Admin (prevalece o mais antigo) |

**Parâmetros do sistema** (`GET/PUT /parametros`, só Admin): prazo padrão (30), máx. em aberto por beneficiário (2),
máx. de renovações (2), janela de renovação (7 dias), dias para inadimplência (30), reserva da fila (48 h),
expirações até sair da fila (2), preventiva (12 meses / 10 ciclos), tentativas de envio (3), intervalo dos avisos
de atraso (3 dias), período padrão de relatórios (30 dias). O prazo **por tipo** fica em `GET/PUT /tipos-equipamento`.

**Fora do escopo desta versão:** recuperação de senha por e-mail, assistente com LLM, reincorporação de item baixado
e contratação do provedor de SMS/WhatsApp (o backend já tem o adaptador Twilio; sem credenciais, o canal vira contato manual).

---

## 🔗 7. API REST — resumo

Base: `http://localhost:8080/api/v1`. Erros no formato *Problem Details* (400 validação, 401, 403, 404, 409 conflito,
413 arquivo grande, 422 regra de negócio, 429 limite de tentativas, 503 serviço externo). Contrato completo, com corpos
e perfis por rota: **[`andamento.md`, seção 6](specs/001-alinhamento-planilha/andamento.md#6-contrato-de-api-previsto-frontend--backend)**.
Swagger: `/swagger-ui.html`.

| Grupo | Rotas principais |
|---|---|
| Autenticação | `POST /auth/login` · `/auth/refresh` · `/auth/logout` · `GET /auth/me` · `PUT /auth/senha` |
| Usuários (A) | `GET/POST /usuarios` · `PUT /usuarios/{id}` · `PATCH /usuarios/{id}/perfil` · `/inativar` · `/ativar` · `/senha` |
| Beneficiários | `GET/POST /beneficiarios` · `GET/PUT /beneficiarios/{id}` · `/consentimento` · `/inativar` · `/reativar` · `/anonimizar` · `/historico` · `/situacao` |
| Pedidos | `POST /pedidos` (público) · `GET /pedidos` · `POST /pedidos/{id}/aprovar` · `/rejeitar` · `/fila` · `/cancelar` · `GET /cep/{cep}` (público) |
| Equipamentos | `GET/PUT /tipos-equipamento` · `GET/POST /equipamentos` · `GET/PUT /equipamentos/{id}` · `/baixa` · `/qrcode` · `/historico` · `GET /equipamentos/leitura?codigo=` |
| Manutenções | `GET/POST /manutencoes` · `PUT /manutencoes/{id}` · `/custos` · `/encerrar` (A) · `GET /manutencoes/preventivas-pendentes` |
| Empréstimos | `GET/POST /emprestimos` · `GET /emprestimos/{id}` · `/termo` · `/termo-assinado` · `/assinatura-digital` · `/assinatura-fisica` · `/renovacao` · `/devolucao` · `/cancelamento` (A) · `/ajustes` · `POST /emprestimos/renovacao-publica` (público) |
| Sincronização | `POST /sincronizacao` · `GET /sincronizacao/conflitos` · `PATCH /sincronizacao/conflitos/{id}/resolver` (A) |
| Fila | `GET/POST /fila` · `PATCH/DELETE /fila/{id}/prioridade` (A) · `POST /fila/{id}/recusa` · `PATCH /fila/{id}/remover` |
| Ocorrências / auditoria | `GET /ocorrencias` · `PATCH /ocorrencias/{id}/resolver` (A) · `GET /auditoria` (A) |
| Notificações | `GET /notificacoes` · `/contatos-manuais` · `PATCH /notificacoes/{id}/concluir` · `GET /notificacoes/resumo-diario` (A) |
| Dashboard / relatórios | `GET /dashboard` · `GET /relatorios/{tipo}?formato=CSV\|PDF` |
| Ajuda / assistente / parâmetros | `GET /ajuda/publico` · `GET/POST /ajuda` · `GET /assistente/sugestoes-alocacao` (A) · `GET /assistente/perguntas` (público) · `GET/PUT /parametros` (A) |

Fluxo principal: pedido público → aprovação com o item → termo PDF → assinatura (empréstimo `ATIVO`) → devolução
(`BOM_ESTADO` libera o item; avaria abre manutenção corretiva e ocorrência) → encerramento APTO pelo Admin → item
reservado automaticamente para o 1º da fila → aprovação do pedido em fila.

---

## ⏰ 8. Rotinas agendadas

Crons em `lions.scheduling.cron.*` (sobrescrevíveis); fuso em `lions.scheduling.zone` (vazio = fuso do servidor, o
mesmo do `Clock` da aplicação). Desligáveis com `lions.scheduling.enabled=false` (já desligadas nos testes).

| Rotina | Quando | Regra |
|---|---|---|
| `OverdueLoanJob` | diária, 01:00 | RN27: marca atrasos e registra inadimplência com 30 dias |
| `PreventiveMaintenanceJob` | diária, 01:30 | RN39: sinaliza revisão preventiva |
| `ReservationExpirationJob` | a cada 15 min | RN33: expira reservas de 48 h e passa ao próximo da fila |
| `NotificationRetryJob` | a cada 15 min (defasada 5 min) | RN48: retentativas de envio |
| `DailySummaryJob` | diária, 07:00 | RN49: resumo diário ao Admin |
| `DueDateReminderJob` | diária, 08:00 | RN46/RF12: régua de lembretes e aviso de renovação |
| `SessionExpirationJob` | a cada 5 min | RN57: encerra e audita sessões expiradas |

---

## 💻 9. Como executar

Pré-requisito: **JDK 21** (ex.: `sdk install java 21.0.11-tem`). Não é preciso instalar o Gradle (use `./gradlew`;
no Windows, `gradlew.bat`).

### Desenvolvimento (perfil `dev`, padrão — H2 em memória)
```bash
cd backend
./gradlew bootRun
```
- Schema gerado pelo Hibernate; 5 equipamentos de exemplo e o admin inicial criados na subida.
- Swagger: http://localhost:8080/swagger-ui.html · Console H2: http://localhost:8080/h2-console
  (JDBC `jdbc:h2:mem:lionsdb`, usuário `sa`, senha vazia).
- Login: `marcio` / `Lions@123`.

### Produção (perfil `prod` — PostgreSQL + Flyway)
```bash
cd backend
./gradlew bootJar
TZ=America/Sao_Paulo SPRING_PROFILES_ACTIVE=prod \
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/<banco> \
SPRING_DATASOURCE_USERNAME=<usuario> SPRING_DATASOURCE_PASSWORD=<senha> \
LIONS_JWT_SECRET=<base64 de 256 bits ou mais> LIONS_ADMIN_PASSWORD=<senha forte> \
java -jar build/libs/ortoempresta-backend-0.0.1-SNAPSHOT.jar
```
O Flyway aplica `db/migration` (`V1`, `V2`) e o Hibernate só **valida** o mapeamento (`ddl-auto: validate`).
Mudanças de schema entram como novas migrations (`V3__...`), nunca editando as existentes. Modelo de dados e
mapeamento lógico → físico: [`docs/banco-de-dados/README.md`](banco-de-dados/README.md).

### Variáveis de ambiente
| Variável | Padrão | Uso |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | `prod` para PostgreSQL + Flyway |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | `jdbc:postgresql://localhost:5432/lionsdb`, `postgres`, `postgres` | Banco (perfil `prod`) |
| `TZ` | fuso do sistema | Use `America/Sao_Paulo`: datas, prazos e rotinas seguem o fuso do servidor |
| `LIONS_SCHEDULING_ZONE` | vazio | Fuso explícito só para as rotinas agendadas |
| `LIONS_JWT_SECRET` | chave de desenvolvimento | **Obrigatório trocar em produção** (Base64, ≥ 256 bits) |
| `LIONS_ADMIN_USERNAME` / `_PASSWORD` / `_EMAIL` | `marcio` / `Lions@123` / `marcio@lionsmedianeira.org.br` | Admin inicial (criado só se não existir) |
| `LIONS_STORAGE_DIR` | `./data/storage` | Termos PDF e documentos assinados |
| `LIONS_NOTIFICATION_MODE` | `auto` | `auto` (envia pelo provedor do canal; sem provedor → contato manual), `manual` ou `simulate` (só log) |
| `LIONS_NOTIFICATION_EMAIL_FROM` | vazio | Remetente dos e-mails |
| `SPRING_MAIL_HOST` / `_PORT` / `_USERNAME` / `_PASSWORD` | — | Servidor SMTP (habilita o canal e-mail) |
| `TWILIO_ACCOUNT_SID` / `TWILIO_AUTH_TOKEN` | vazio | Credenciais Twilio (habilitam SMS/WhatsApp) |
| `TWILIO_SMS_FROM` / `TWILIO_WHATSAPP_FROM` | vazio | Números de origem de SMS e WhatsApp |
| `LIONS_CORS_ALLOWEDORIGINS` | `*` | Origens permitidas (lista separada por vírgula) |

### Testes
```bash
cd backend
./gradlew test
```
Rodam em H2 com as rotinas desligadas. A mesma suíte roda contra PostgreSQL apontando para um banco vazio:
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/<banco> SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver \
SPRING_DATASOURCE_USERNAME=postgres SPRING_DATASOURCE_PASSWORD= SPRING_JPA_HIBERNATE_DDL_AUTO=validate \
SPRING_FLYWAY_ENABLED=true ./gradlew cleanTest test
```
Estado em 24/11/2026: **239 testes verdes** em H2 e em PostgreSQL 16, incluindo o teste ponta a ponta pela API
(`FluxoPontaAPontaIT`).

---

## 🚀 10. Próximos passos

- Seed de conteúdo de ajuda no perfil `dev` (tarefa A.3).
- Ampliar os testes automatizados das RNs (parte das regras foi verificada só pelos testes de integração).
- Propostas para o frontend: `GET /dominios` (enums para selects) e upload de fotos de entrega/devolução.
- Infraestrutura de deploy (Docker, Nginx, VPS) e contratação do provedor de SMS/WhatsApp.
