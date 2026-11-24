# Andamento da implementação — Spec 001 (backend)

> **Atualizado em 24/11/2026 (Onda 2):** integração concluída — teste ponta a ponta pela API (`FluxoPontaAPontaIT`),
> suíte verde em H2 e PostgreSQL (239 testes) e documentação atualizada (I.1–I.4).
> **Atualizado em 24/11/2026:** controllers REST da Onda 1 prontos (ver seções 1, 2 e 5).
> **Revisão de 24/11/2026 (alinhamento planilha/MER):** spec revisada (itens **[rev. 24/11]** do `spec.md`), MER
> ajustado (`docs/banco-de-dados/`), script PostgreSQL `V1__init_schema.sql`, e no código: só CPF, endereço em tabela
> própria, tipos de equipamento com prazo, ano/especificações do equipamento. Ver Frente G do `tasks.md`.
> **Snapshot original:** 18/11/2026 · branch `wip/onda1-servicos` (criada a partir de `new/one` @ `baa36fa`).
> Documento gerado quando a execução foi **interrompida no meio da Onda 1**. Descreve o que está pronto, o que falta,
> as decisões tomadas e o **contrato de API previsto** para o frontend.
> Implementação feita **sem TDD** (a pedido): nenhum teste novo foi escrito nesta etapa.

---

## 1. Resumo

| Onda | Situação |
|---|---|
| **Onda 0 — Fundação** | ✅ Concluída (já estava pronta na branch). Verificado antes das mudanças: build ok e **172 testes verdes**. |
| **Frente G — Alinhamento planilha/MER** | ✅ G.1–G.14 concluídas (modelo, V1/V2, MER e ajustes de regra da revisão). |
| **Onda 1 — Frentes A–F** | ✅ **Concluída.** Serviços e controllers REST das frentes A–F, as 7 rotinas agendadas, régua de lembretes, resumo diário, dashboard e relatórios. Pendente só o seed de ajuda em `dev` (A.3). |
| **Onda 2 — Integração** | ✅ **Concluída.** I.1 merge conferido (sem conflitos; imports sem uso removidos), I.2 teste ponta a ponta pela API, I.3 suíte verde em H2 e PostgreSQL, I.4 `README.md`, `backend/README.md` e `docs/DESCRICAO_DO_PROJETO_E_BACKEND.md` atualizados. |

**Estado da build (24/11/2026, após a Onda 2):** ✅ `./gradlew test` com **239 testes verdes** em H2 e também
contra **PostgreSQL 16** (banco vazio, `V1` + `V2` aplicadas pelo Flyway, `ddl-auto: validate`). Os 3 testes novos
(`FluxoPontaAPontaIT`) exercitam só pela API HTTP, com login JWT real e commit a cada requisição: pedido público →
aprovação → termo PDF → assinatura digital → fila sem estoque → devolução DANIFICADO → manutenção corretiva automática
+ ocorrência DANO (bloqueio RN20) → encerramento negado ao Operador e APTO pelo Admin (UC23) → reserva automática para
o 1º da fila (RN32) → aprovação do pedido em fila; histórico do equipamento; dashboard e relatório CSV com visão
mascarada para Consulta e auditoria `EXPORT`; renovação pública (UC16); sincronização offline com conflito,
idempotência e resolução pelo Admin (RN60). A consulta de CEP responde 503 quando o ViaCEP não está acessível
(comportamento previsto).

**O que funciona hoje em execução (HTTP):** todas as rotas ✅ da seção 6 e as 7 rotinas agendadas.

---

## 2. Status por tarefa (`tasks.md`)

Legenda: ✅ serviço + rota prontos (testes automatizados das RNs ainda pendentes) · 🟡 falta job/seed indicado · ❌ não iniciado

### Frente A — Acesso, ajuda e assistente
| Tarefa | Status | Onde |
|---|---|---|
| A.1 Login/refresh/logout/troca de senha (RN57) + auditoria | ✅ | `application/service/auth/AuthService` |
| A.2 CRUD de usuários, perfil, inativação, redefinição de senha (RN01–RN03) | ✅ | `application/service/user/UserService` |
| A.3 Conteúdo de ajuda (RN53) + FAQ pública + seed `dev` | 🟡 (seed `dev` ❌) | `application/service/help/HelpContentService` |
| A.4 Assistente por regras: sugestão de alocação + busca de FAQ (RN50, RN51) | ✅ | `application/service/assistant/AssistantService` |

### Frente B — Beneficiários, solicitações e histórico
| Tarefa | Status | Onde |
|---|---|---|
| B.1 CRUD + consentimento + inativação/reativação + anonimização (RN03–RN08, RN34, RN59) | ✅ | `application/service/beneficiary/BeneficiaryService` |
| B.2 Histórico consolidado (RN42) | ✅ | `BeneficiaryService.history` |
| B.3 Pedido público + aprovação (→ `LoanService.registerLoan`) + rejeição + fila (+ cancelamento) | ✅ | `application/service/order/LoanOrderService` |
| B.4 Consulta CEP | ✅ | `AddressService` + `infrastructure/client/ViaCepAddressLookupAdapter` |

### Frente C — Equipamentos, QR Code e manutenção
| Tarefa | Status | Onde |
|---|---|---|
| C.1 Cadastro (patrimônio gerado), edição restrita, filtros, baixa (RN09, RN11–RN14) | ✅ | `application/service/equipment/EquipmentService` |
| C.2 QR Code PNG + leitura com operações disponíveis (RN10, RN15) | ✅ | `EquipmentService.qrCode/scan` + `infrastructure/qrcode/ZxingQrCodeAdapter` |
| C.3 Manutenção: abertura, custos/ajustes, encerramento, listener de avaria (RN24, RN35–RN38, RN41) | ✅ | `application/service/maintenance/MaintenanceService` |
| C.4 Preventiva `findDue` + job (RN39) | ✅ (`PreventiveMaintenanceJob`, diário 01:30) | `PreventiveMaintenanceService.findDue/notifyDue` |
| C.5 Histórico do equipamento | ✅ | `EquipmentService.history` |

### Frente D — Empréstimos, termo, ocorrências e auditoria
| Tarefa | Status | Onde |
|---|---|---|
| D.1 Registro de empréstimo (RN16–RN22, RN28) + termo PDF armazenado (RN06, RN21, RN61) | ✅ | `application/service/loan/LoanService.registerLoan` |
| D.2 Assinatura digital e física (RN21, RN25) | ✅ | `LoanService.registerDigitalSignature/registerPhysicalSignature` |
| D.3 Renovação (RN23), devolução + ocorrência + eventos (RN24, RN40), cancelamento (RN26), ajustes (RN41) | ✅ | `LoanService.renew/registerReturn/cancel/addAdjustment` |
| D.4 `BeneficiaryRestrictionService` (RN08, RN20) | ✅ | `application/service/loan/BeneficiaryRestrictionService` |
| D.5 Job de atraso/inadimplência (RN27) + ocorrências (listar/resolver) | ✅ (`OverdueLoanJob`, diário 01:00) | `LoanOverdueService`, `OccurrenceService` |
| D.6 Consulta de auditoria (RN58) + histórico de empréstimos com filtros (RF10) | ✅ | `AuditService.search` (Onda 0), `LoanService.findAll` |

### Frente E — Fila de espera e notificações
| Tarefa | Status | Onde |
|---|---|---|
| E.1 Entrada na fila, ordenação, prioridade, recusa, remoção (RN29–RN31, RN33, RN34) | ✅ | `application/service/waitlist/WaitlistService` |
| E.2 Reserva automática por evento + expiração (RN32, RN33) | ✅ (`ReservationExpirationJob`, a cada 15 min) | `WaitlistService.onEquipmentAvailable/expireReservations` |
| E.3 Envio por canal autorizado, porta + adaptador log/wa.me, retentativas, contato manual (RN47, RN48) | ✅ (`NotificationRetryJob`, a cada 15 min; envio real é a G.12) | `NotificationService`, `application/port/NotificationSenderPort`, `infrastructure/notification/LogNotificationSenderAdapter` |
| E.4 Régua de lembretes (RN46) e resumo diário (RN49) | ✅ | `DueDateReminderService` + `DueDateReminderJob` (08:00), `DailySummaryService` + `DailySummaryJob` (07:00) |

### Frente F — Dashboard e relatórios
| Tarefa | Status |
|---|---|
| F.1 Indicadores com filtros e visão por perfil (RN54–RN56) | ✅ `application/service/dashboard/DashboardService` |
| F.2 Relatórios CSV/PDF, mascaramento, período padrão, auditoria de exportação (RN43–RN45, RN55) | ✅ `application/service/report/ReportService` (+ `CsvReportWriter`, `PdfReportWriter`) |

### Contratos entre módulos (`plan.md §6`)
Todos os esqueletos que lançavam `UnsupportedOperationException` foram **implementados**:
`LoanService.registerLoan`, `BeneficiaryRestrictionService.evaluate`, `WaitlistService.enqueue/hasWaitingFor/
hasActiveEntry/markServed`, `NotificationService.notifyBeneficiary/notifyAdmins`, `PreventiveMaintenanceService.findDue`.

| Evento | Publicado por (implementado) | Consumido por (implementado) |
|---|---|---|
| `EquipmentAvailableEvent` | cadastro de equipamento, manutenção APTO, devolução BOM_ESTADO, cancelamento de empréstimo | `WaitlistService.onEquipmentAvailable` → reserva para o 1º da fila (RN32) |
| `EquipmentDamagedOnReturnEvent` | devolução PRECISA_MANUTENCAO/DANIFICADO | `MaintenanceService.onEquipmentDamagedOnReturn` → corretiva automática (RN24) |
| `BeneficiaryRestrictedEvent` | inativação, anonimização, atraso (RN27), ocorrência de dano/inadimplência | `WaitlistService.onBeneficiaryRestricted` → remove da fila (RN34) |

---

## 3. Arquivos alterados neste snapshot

**Movidos** (com `git mv`): `service/AuthService` → `service/auth/`, `service/EquipmentService` → `service/equipment/`,
`service/LoanOrderService` → `service/order/`, `service/PdfTermService` → `service/loan/` (+ cláusula de gratuidade, RN16).

**Removidos (DTOs pré-spec sem uso):** `LoanApprovalRequest`, `TermAcceptanceRequest`, `MaintenanceRequest`,
`DashboardIndicatorsResponse`.
**Reescritos:** `AuthResponse`, `LoanOrderCreateRequest`, `LoanReturnRequest`, `LoanReturnResponse`, `MaintenanceResponse`.

**Novos serviços:** `assistant/AssistantService`, `beneficiary/BeneficiaryService`, `beneficiary/AddressService`,
`help/HelpContentService`, `loan/LoanOverdueService`, `loan/OccurrenceService`, `maintenance/MaintenanceService`,
`user/UserService`.
**Esqueletos implementados:** `loan/LoanService`, `loan/BeneficiaryRestrictionService`, `waitlist/WaitlistService`,
`notification/NotificationService`, `maintenance/PreventiveMaintenanceService`.

**Novos DTOs (request):** `AdjustmentRequest`, `BeneficiaryRequest`, `ConsentRequest`, `DigitalSignatureRequest`,
`HelpContentRequest`, `LoanOrderApprovalRequest`, `MaintenanceCloseRequest`, `MaintenanceCostRequest`,
`MaintenanceCreateRequest`, `MaintenanceUpdateRequest`, `OccurrenceResolveRequest`, `PasswordChangeRequest`,
`PasswordResetRequest`, `ReasonRequest`, `UserCreateRequest`, `UserRoleRequest`, `UserUpdateRequest`,
`WaitlistPriorityRequest`, `WriteOffRequest`.
**Novos DTOs (response):** `AdjustmentResponse`, `AllocationSuggestionsResponse`, `AssistantAnswerResponse`,
`AuditLogResponse`, `BeneficiaryHistoryResponse`, `BeneficiaryResponse`, `BeneficiarySummaryResponse`,
`CepAddressResponse`, `EquipmentHistoryResponse`, `EquipmentOperation`, `EquipmentScanResponse`, `HelpContentResponse`,
`LoanOrderCreatedResponse`, `LoanOrderDecisionResponse`, `LoanRenewalResponse`, `MaintenanceCostResponse`,
`NotificationResponse`, `OccurrenceResponse`, `UserResponse`. Acréscimos em `LoanResponse`, `WaitlistEntryResponse`
e `BeneficiaryRestriction`.

**Portas e adaptadores novos:** `AddressLookupPort` → `ViaCepAddressLookupAdapter`; `NotificationSenderPort` →
`LogNotificationSenderAdapter`; `QrCodePort` → `ZxingQrCodeAdapter`.

**Acréscimos na fundação (sem quebrar contratos):**
- `domain.exception`: `ForbiddenOperationException` (→ 403) e `ExternalServiceException` (→ 503); `GlobalExceptionHandler`
  também trata upload acima do limite (→ 413).
- `Equipment.cancelLoan()` e `Equipment.isReservedFor(id)`.
- `Notification.referenceKey` (+ coluna `reference_key`) e `Notification.requireManualContact(...)`.
- Ports: `BeneficiaryRepositoryPort.findByDocumentNumber/countByActive`;
  `NotificationRepositoryPort.existsByTypeAndReferenceKeyAndReferenceDate/findByBeneficiaryId` (+ Spring Data e adapters).
- `AuditService.recordAs(...)` (auditar em nome de alguém — login, pedido público) e `recordWithRule(...)`.
- `JwtTokenProvider.generateToken(username, perfis)` (refresh de sessão).
- `WhatsAppMessageHelper.dueTodayMessage/loanRegisteredMessage`; utilitário `application/support/AuditDetails`.

---

## 4. Decisões tomadas (para revisão da equipe)

1. **Campos JSON em inglês** (plan §1) também nos corpos que o `plan.md §7` escreveu em português: `{reason}` em vez de
   `{motivo}`, `{note}` em vez de `{nota}`, `{description, reason}` em vez de `{descricao, motivo}`.
   Rotas e query params continuam em português.
2. **Equipamento indisponível no empréstimo → 409** para qualquer status não emprestável (já emprestado, reservado para
   outro, em manutenção, baixado). Cobre a corrida concorrente da RN28 (lock pessimista + `@Version`).
3. **Regras de perfil dentro do caso de uso → 403** (`ForbiddenOperationException`): empréstimo retroativo (RN22), ajuste
   de custo (RN41), encerramento INSERVÍVEL (RN37). Demais permissões ficam no `@PreAuthorize` dos controllers.
4. *(Revogada na revisão de 24/11 — G.12: modo padrão agora é `auto` — e-mail por SMTP e SMS/WhatsApp pela Twilio
   quando configurados; contato manual só como contingência. `manual` e `simulate` continuam disponíveis.)*
   **Notificações sem provedor real:** propriedade `lions.notification.mode` — `manual` (padrão: toda notificação ao
   beneficiário vira **tarefa de contato manual com link wa.me pronto**, o botão "Lembrar no WhatsApp") ou `simulate`
   (só registra em log e marca como ENVIADA). Retentativas (RN48) passam a valer quando um provedor real for plugado na
   porta `NotificationSenderPort`.
5. **Deduplicação sem empréstimo** via `Notification.referenceKey` (ex.: preventiva uma vez por item/mês — `EQUIPMENT:12`).
6. *(Revogada na revisão de 24/11 — G.7: termo pendente também remove da fila, RN34.)*
   **`BeneficiaryRestriction`** ganhou `hasOverdueLoan`, `hasPendingSignature`, `inactive` e `removesFromWaitlist()`:
   termo pendente de assinatura **bloqueia** novas entradas (RN20), mas **não remove** quem já está na fila; atraso,
   ocorrência aberta e cadastro inativo removem (RN34). O construtor original de 4 argumentos foi mantido.
7. **Pedido público:** se já existe beneficiário **ativo** com o documento, o pedido é vinculado a ele **sem alterar o
   cadastro** (rota anônima não pode sobrescrever dados). A resposta é só um **protocolo** (sem dados pessoais) e os
   Admins recebem notificação interna. Usuário autenticado pode usar `beneficiaryId` (cadastro assistido).
8. **Aprovação** do pedido delega para `LoanService.registerLoan`, que aprova a solicitação e marca a posição da fila
   como ATENDIDA (se ele tinha reserva de outro item do mesmo tipo, a reserva é liberada para o próximo).
9. **Entrar na fila com item disponível** do tipo dispara a reserva na hora (RN32). Recusa/expiração **não** re-reserva o
   mesmo item para quem acabou de recusar.
10. **Assinatura digital:** `signerName` opcional (padrão = signatário do RN06); quando exige responsável legal, o nome
    precisa coincidir (comparação sem acento/caixa).
11. **Arquivos de termo** recebem timestamp no nome (evita colisão de ids entre reinícios do H2 em memória com storage
    persistente em `dev`).
12. **Baixa de item em manutenção** encerra a manutenção aberta com parecer INSERVÍVEL (RN13 + RN37).
13. **Ajustes de empréstimo (RN41)** ficam na trilha de auditoria (ação `ADJUSTMENT`) e aparecem no detalhe do empréstimo.
14. **Anonimização (RN59)** também limpa o motivo de saúde e o nome do solicitante das solicitações do beneficiário.

---

## 5. Pendências — próximos passos (em ordem sugerida)

1. ~~**Controllers REST**~~ ✅ feito em 24/11/2026 (14 controllers + `AuditService.searchResponses`). Acréscimos ao
   contrato: `GET /ajuda/{id}` (A), `GET /fila/{id}`, `GET /ocorrencias/{id}` e `GET /emprestimos/{id}/ajustes` (A,O).
2. **Rotinas agendadas:** ✅ todas as 6 do `plan.md §8` (crons em `lions.scheduling.cron.*`).
3. ~~**E.4**~~ ✅ feito: serviço da régua de lembretes (RN46: 7 dias, 1 dia, no dia, a cada 3 dias de atraso, dedupe diário) e do
   resumo diário (RN49) + `GET /notificacoes/resumo-diario`.
4. ~~**Frente F**~~ ✅ feito: `DashboardService` (RN54–RN56) e `ReportService` (CSV/PDF, mascaramento p/ Consulta, auditoria `EXPORT`).
5. ~~**Banco:** migration Flyway~~ ✅ `V1__init_schema.sql` (PostgreSQL) — ver `docs/banco-de-dados/README.md`.
   **Configuração:** `lions.notification.mode` e `spring.servlet.multipart.max-file-size/max-request-size` no
   `application.yml`; seed de ajuda no perfil `dev`.
6. **Propostas para o frontend** (fora do `plan.md`): `GET /api/v1/dominios` (enums + descrições, público) e upload de
   fotos `POST /api/v1/arquivos/fotos` (as URLs vão em `photoUrl`, `deliveryPhotoUrl`, `returnPhotoUrl`).
7. ~~Ajustar `SecurityAndParameterIT.devePermitirRotasPublicasQuandoAnonimo`~~ ✅ agora espera 400/200.
8. ~~**Onda 2:** teste ponta a ponta (I.2), suíte verde (I.3) e docs (I.4)~~ ✅ feito em 24/11/2026.
9. **Testes das RNs** (adiados por decisão — sem TDD nesta etapa).
10. ~~**Frente G**~~ ✅ concluída (G.1–G.14).

> **Ambiente:** o toolchain do Gradle exige **JDK 21** (`gradle-daemon-jvm.properties`). Sem JDK 21 instalado a build
> não inicia. Sugestão: instalar o Temurin 21 ou adicionar o plugin `org.gradle.toolchains.foojay-resolver-convention`
> no `settings.gradle.kts` para download automático.

---

## 6. Contrato de API previsto (frontend ↔ backend)

> Rotas ✅ expostas pelos controllers em `presentation/controller` (verificadas ponta a ponta em 24/11/2026).
> Coluna **Status**: ✅ disponível · ❌ não iniciado (só propostas).

### 6.1 Convenções (já implementadas na fundação)

- **Base URL:** `http://localhost:8080/api/v1` (dev). Swagger: `/swagger-ui.html`; OpenAPI: `/v3/api-docs`.
- **Autenticação:** JWT HS256 no header `Authorization: Bearer <token>`. Validade **30 min** (`expiresIn` em segundos
  e `expiresAt` em UTC na resposta do login). O frontend deve chamar `POST /auth/refresh` enquanto houver atividade
  (ex.: faltando < 5 min) — sem atividade a sessão expira (RN57). `POST /auth/logout` encerra a sessão. Sessões são
  persistidas: token de sessão encerrada (logout, renovação, usuário inativado, expiração) recebe 401.
- **Perfis:** `ROLE_ADMIN` (A), `ROLE_OPERATOR` (O), `ROLE_VIEWER` (V); P = público (sem token).
- **Respostas de erro** no formato *Problem Details* (RFC 7807), `Content-Type: application/problem+json`:
  ```json
  { "type": "about:blank", "title": "Dados inválidos", "status": 400,
    "detail": "Um ou mais campos são inválidos.", "instance": "/api/v1/beneficiarios",
    "errors": { "birthDate": "A data de nascimento é obrigatória.", "beneficiary.cep": "CEP inválido (use 00000-000)." } }
  ```
  | HTTP | Quando | O que o frontend faz |
  |---|---|---|
  | 400 | validação de campos (`errors`: campo → mensagem) | marcar campos do formulário |
  | 401 | sem token / token inválido, expirado ou revogado / login inválido | ir para o login |
  | 403 | perfil sem permissão (rota ou regra, ex.: retroativo sem ser Admin) | mostrar `detail` |
  | 404 | recurso inexistente | mostrar `detail` |
  | 409 | conflito de estado (duplicidade, item indisponível, concorrência); pode trazer extras, ex. `existingId` | mostrar `detail`; oferecer abrir o existente |
  | 413 | arquivo acima do limite | mostrar `detail` |
  | 422 | regra de negócio violada (RNxx) | mostrar `detail` |
  | 503 | serviço externo indisponível (ViaCEP) | permitir preenchimento manual |
  | 500 | erro inesperado | mensagem genérica |
- **Datas:** `LocalDate` = `"2026-11-18"`; `LocalDateTime` = `"2026-11-18T14:30:00"` (horário do servidor, sem fuso);
  `expiresAt` = instante UTC (`"2026-11-18T17:30:00Z"`).
- **Enums** trafegam pelo nome (`"CADEIRA_DE_RODAS"`); as respostas trazem `*Description` com o rótulo em português.
- **Listas** são arrays simples (sem paginação nesta versão).
- **Arquivos** (PDF do termo, PNG do QR Code, relatórios): baixar via `fetch` com o header `Authorization` e abrir como
  *blob* (`<img src>`/`<a href>` direto não enviam o token). O CORS expõe `Content-Disposition`.
- **CORS:** origens em `lions.cors.allowed-origins` (padrão `*` em dev).

### 6.2 Endpoints

#### Autenticação e usuários (RF01)
| Método e rota | Perfis | Corpo → Resposta | Status |
|---|---|---|---|
| `POST /auth/login` | P | `{username, password}` (usuário ou e-mail) → `AuthResponse {token, type:"Bearer", expiresIn, expiresAt, user}` | ✅ |
| `POST /auth/refresh` | A,O,V | — → `AuthResponse` (o token anterior é revogado) | ✅ |
| `POST /auth/logout` | A,O,V | — → 204 | ✅ |
| `GET /auth/me` *(acréscimo)* | A,O,V | — → `UserResponse` | ✅ |
| `PUT /auth/senha` | A,O,V | `{currentPassword, newPassword}` → 204 (senha forte, RN57) | ✅ |
| `GET /usuarios?perfil&ativo` · `GET /usuarios/{id}` | A | → `UserResponse[]` / `UserResponse` | ✅ |
| `POST /usuarios` | A | `{username, fullName?, email, password, role}` → 201 `UserResponse` | ✅ |
| `PUT /usuarios/{id}` | A | `{fullName?, email}` → `UserResponse` | ✅ |
| `PATCH /usuarios/{id}/perfil` | A | `{role}` → `UserResponse` (nunca o próprio — 422) | ✅ |
| `PATCH /usuarios/{id}/inativar` · `/ativar` | A | → `UserResponse` (não a si; mantém ≥ 1 Admin) | ✅ |
| `PATCH /usuarios/{id}/senha` | A | `{newPassword}` → 204 | ✅ |

`UserResponse`: `{id, username, fullName, email, role, roleDescription, active, lastLoginAt, createdAt, updatedAt}`.

#### Beneficiários, solicitações e CEP (RF02, RF09)
| Método e rota | Perfis | Corpo → Resposta | Status |
|---|---|---|---|
| `POST /beneficiarios` | A,O | `BeneficiaryRequest` → 201 `BeneficiaryResponse` (409 + `existingId` se CPF ativo duplicado) | ✅ |
| `GET /beneficiarios?nome&documento&ativo` | A,O | → `BeneficiarySummaryResponse[]` (`maskedCpf`; `documento` = CPF com ou sem máscara) | ✅ |
| `GET /beneficiarios/{id}` | A,O | → `BeneficiaryResponse` | ✅ |
| `PUT /beneficiarios/{id}` | A,O | `BeneficiaryRequest` (consentimento ignorado) → `BeneficiaryResponse` | ✅ |
| `PUT /beneficiarios/{id}/consentimento` | A,O | `{consentGranted:true, notificationChannels[], consentGivenBy?}` → `BeneficiaryResponse` | ✅ |
| `PATCH /beneficiarios/{id}/inativar` | A | `{reason}` → `BeneficiaryResponse` (422 se empréstimo/ocorrência/fila — RN08) | ✅ |
| `PATCH /beneficiarios/{id}/reativar` | A | → `BeneficiaryResponse` | ✅ |
| `POST /beneficiarios/{id}/anonimizar` | A | → `BeneficiaryResponse` | ✅ |
| `GET /beneficiarios/{id}/historico` | A,O | → `{beneficiary, restriction, loans[], orders[], waitlist[], occurrences[]}` | ✅ |
| `GET /beneficiarios/{id}/situacao` *(acréscimo)* | A,O | → `BeneficiaryRestriction {blocked, reasons[], ...}` | ✅ |
| `POST /pedidos` | **P** | `LoanOrderCreateRequest` → 201 `{id, status, statusDescription, createdAt, message}` | ✅ |
| `GET /pedidos?status&tipo&beneficiarioId` · `GET /pedidos/{id}` | A,O | → `LoanOrderResponse[]` / `LoanOrderResponse` | ✅ |
| `POST /pedidos/{id}/aprovar` | A,O | `{equipmentId, loanDate?, retroactiveJustification?, deliveryNotes?, deliveryPhotoUrl?}` → `{order, loan}` | ✅ |
| `POST /pedidos/{id}/rejeitar` | A,O | `{reason}` → `LoanOrderResponse` | ✅ |
| `POST /pedidos/{id}/fila` | A,O | — → `{order, waitlistEntry}` | ✅ |
| `POST /pedidos/{id}/cancelar` *(acréscimo)* | A,O | `{reason}` → `LoanOrderResponse` | ✅ |
| `GET /cep/{cep}` | **P** | → `{cep, street, complement, neighborhood, city, state, ibgeCode, ddd}` (404 inexistente, 503 indisponível) | ✅ |

`BeneficiaryRequest` (RN04–RN07):
```json
{
  "fullName": "Maria Aparecida da Silva", "cpf": "529.982.247-25",
  "birthDate": "1950-04-12", "phone": "(45) 99999-1234", "email": null,
  "cep": "85884-000", "street": "Rua Argentina", "number": "100", "complement": null,
  "neighborhood": "Centro", "city": "Medianeira", "state": "PR",
  "alternativeContactName": "João da Silva", "alternativeContactPhone": "(45) 98888-0000",
  "alternativeContactRelationship": "Filho",
  "legallyIncapable": false, "legalGuardianName": null, "legalGuardianCpf": null,
  "legalGuardianPhone": null, "legalGuardianEmail": null, "legalGuardianRelationship": null, "notes": null,
  "consentGranted": true, "consentGivenBy": null, "notificationChannels": ["WHATSAPP"]
}
```
Só CPF (RN04, sem `documentType`); `number` aceita "S/N". Menor de 18 anos ou `legallyIncapable=true` exige
`legalGuardianName` + `legalGuardianCpf` válido (RN06). Respostas trazem `cpf`/`maskedCpf` (antes `documentNumber`/`maskedDocument`).

`LoanOrderCreateRequest` (formulário público em 3 etapas):
```json
{ "forSelf": false, "requesterName": "Ana Souza", "requesterRelationship": "Filha",
  "beneficiary": { "...": "BeneficiaryRequest acima" },
  "equipmentType": "CADEIRA_DE_RODAS", "needReason": "Pós-operatório de fêmur" }
```

#### Equipamentos, QR Code e manutenção (RF03, RF04, RF08)
| Método e rota | Perfis | Corpo → Resposta | Status |
|---|---|---|---|
| `GET /tipos-equipamento` *(rev. 24/11)* | A,O,V | → `[{code, description, loanDays, effectiveLoanDays, updatedBy, updatedAt}]` | ✅ |
| `PUT /tipos-equipamento/{codigo}` *(rev. 24/11)* | A | `{description, loanDays?}` (nulo = prazo padrão) → `EquipmentTypeResponse` | ✅ |
| `POST /equipamentos` | A,O | `{type, size?, manufactureYear?, specifications?: [{name, value}], condition, notes?, photoUrl?}` → 201 `EquipmentResponse` (patrimônio `PT-0001` gerado) | ✅ |
| `GET /equipamentos?status&tipo&condicao` | A,O,V | → `EquipmentResponse[]` | ✅ |
| `GET /equipamentos/{id}` · `GET /equipamentos/patrimonio/{numero}` | A,O,V | → `EquipmentResponse` | ✅ |
| `PUT /equipamentos/{id}` | A,O | `{type?, size?, manufactureYear?, specifications?, condition?, notes?, photoUrl?}` (`specifications` substitui a lista) → `EquipmentResponse` | ✅ |
| `POST /equipamentos/{id}/baixa` | A | `{reason: INSERVIVEL\|PERDA\|FURTO\|DOACAO, notes?}` → `EquipmentResponse` | ✅ |
| `GET /equipamentos/{id}/qrcode?tamanho=300` | A,O | → `image/png` (conteúdo = número de patrimônio) | ✅ |
| `GET /equipamentos/leitura?codigo=PT-0001` | A,O | → `{equipment, availableOperations[], currentLoanId, currentBeneficiaryName, expectedReturnDate, openMaintenanceId, reservedForBeneficiaryName, ...}` | ✅ |
| `GET /equipamentos/{id}/historico` | A,O | → `{equipment, loans[], maintenances[], loanCount, totalMaintenanceCost}` | ✅ |
| `POST /manutencoes` | A,O | `{equipmentId, type?, defectDescription, entryDate?, technicalResponsible}` → 201 `MaintenanceResponse` | ✅ |
| `GET /manutencoes?status&equipamentoId&tipo` · `GET /manutencoes/{id}` | A,O | → `MaintenanceResponse[]` / `MaintenanceResponse` | ✅ |
| `PUT /manutencoes/{id}` *(acréscimo)* | A,O | `{defectDescription?, technicalResponsible?}` (só em andamento) | ✅ |
| `POST /manutencoes/{id}/custos` | A,O (ajuste só A) | `{type: PECA\|SERVICO, description, partReplaced?, amount, adjustment?, adjustmentReason?}` | ✅ |
| `POST /manutencoes/{id}/encerrar` | **A** (UC23) | `{verdict: APTO\|INSERVIVEL, notes?}` → `MaintenanceResponse` | ✅ |
| `GET /manutencoes/preventivas-pendentes` | A,O | → `[{equipmentId, patrimonyNumber, type, lastMaintenanceDate, loanCycles, reason}]` | ✅ |

`availableOperations` (RN15): `EMPRESTAR`, `REGISTRAR_ASSINATURA`, `REGISTRAR_DEVOLUCAO`, `RENOVAR`, `ABRIR_MANUTENCAO`,
`LANCAR_CUSTO`, `ENCERRAR_MANUTENCAO`, `BAIXAR` (só Admin), `EDITAR`, `IMPRIMIR_QRCODE`, `VER_HISTORICO`.

#### Empréstimos, termo, ocorrências e auditoria (RF05, RF07, RF10)
| Método e rota | Perfis | Corpo → Resposta | Status |
|---|---|---|---|
| `POST /emprestimos` | A,O (retroativo só A) | `{beneficiaryId, equipmentId, orderId?, loanDate?, retroactiveJustification?, deliveryNotes?, deliveryPhotoUrl?}` → 201 `LoanResponse` (status `PENDENTE_ASSINATURA`) | ✅ |
| `GET /emprestimos?status&beneficiarioId&equipamentoId&inicio&fim&tipo` | A,O | → `LoanResponse[]` | ✅ |
| `POST /emprestimos/renovacao-publica` | **P** | `{cpf, birthDate, patrimonyNumber}` → `{patrimonyNumber, equipmentTypeDescription, previousExpectedReturnDate, expectedReturnDate, renewalCount, maxRenewals}` (UC16; 422 genérico se não confere; 429 após 5 tentativas/15 min por IP ou CPF) | ✅ |
| `POST /sincronizacao` | A,O | `{operations: [{clientOperationId, type: RETIRADA\|DEVOLUCAO\|RENOVACAO, occurredAt, beneficiaryId?, equipmentId?, loanId?, returnCondition?, damageDescription?, deliveryNotes?}]}` (máx. 200) → `[{clientOperationId, type, status: APLICADA\|RECUSADA\|CONFLITO\|DUPLICADA, message, loanId, conflictId}]` (RN60) | ✅ |
| `GET /sincronizacao/conflitos?status` · `GET /sincronizacao/conflitos/{id}` | A,O | → `SyncConflictResponse[]` / `SyncConflictResponse` | ✅ |
| `PATCH /sincronizacao/conflitos/{id}/resolver` | A | `{note}` → `SyncConflictResponse` | ✅ |
| `GET /emprestimos/{id}` | A,O | → `LoanResponse` com `renewals[]`, `returnInfo`, `adjustments[]`, `canRenew`, `renewalBlockers[]`, `whatsappLink` | ✅ |
| `GET /emprestimos/{id}/termo` | A,O | → `application/pdf` (RN61) | ✅ |
| `GET /emprestimos/{id}/termo-assinado` *(acréscimo)* | A,O | → PDF/JPG/PNG digitalizado | ✅ |
| `POST /emprestimos/{id}/assinatura-digital` | A,O | `{signerName?}` → `LoanResponse` (status `ATIVO`) | ✅ |
| `POST /emprestimos/{id}/assinatura-fisica` | A,O | `multipart/form-data`: `file` (PDF/JPG/PNG ≤ 10 MB) + `signerName?` → `LoanResponse` | ✅ |
| `POST /emprestimos/{id}/renovacao` | A,O | — → `LoanResponse` (RN23) | ✅ |
| `POST /emprestimos/{id}/devolucao` | A,O | `{returnCondition: BOM_ESTADO\|PRECISA_MANUTENCAO\|DANIFICADO, damageDescription?, returnPhotoUrl?, returnDate?}` → `LoanResponse` | ✅ |
| `POST /emprestimos/{id}/cancelamento` | A | `{reason}` → `LoanResponse` | ✅ |
| `POST /emprestimos/{id}/ajustes` | A | `{description, reason}` → `{id, description, reason, registeredBy, registeredAt}` | ✅ |
| `GET /ocorrencias?beneficiarioId&status&tipo` | A,O | → `OccurrenceResponse[]` | ✅ |
| `PATCH /ocorrencias/{id}/resolver` | A | `{note}` → `OccurrenceResponse` | ✅ |
| `GET /auditoria?entidade&entidadeId&inicio&fim` | A | → `[{id, entityName, entityId, action, actionDescription, username, automatic, originRule, details, occurredAt}]` | ✅ |

`LoanResponse` (principais campos): `id, beneficiaryId, beneficiaryName, beneficiaryPhone, equipmentId, patrimonyNumber,
equipmentType, orderId, status, statusDescription, loanDate, expectedReturnDate, renewalCount, retroactive,
termAvailable, termHashSha256, signatoryName, requiresLegalGuardian, signatureType, signedAt, signedByName,
signedDocumentAvailable, overdue, daysOverdue, dueSoon, returnedAt, cancelReason`.

#### Fila de espera e notificações (RF06, RF12)
| Método e rota | Perfis | Corpo → Resposta | Status |
|---|---|---|---|
| `POST /fila` | A,O | `{beneficiaryId, equipmentType, orderId?}` → 201 `WaitlistEntryResponse` (409 se já na fila do tipo) | ✅ |
| `GET /fila?tipo&status` | A,O | → `WaitlistEntryResponse[]` (sem `status` = AGUARDANDO+RESERVADO, na ordem efetiva, com `position`) | ✅ |
| `PATCH /fila/{id}/prioridade` · `DELETE /fila/{id}/prioridade` | A | `{reason: URGENCIA_SAUDE\|PESSOA_IDOSA\|POS_OPERATORIO, justification}` | ✅ |
| `POST /fila/{id}/recusa` | A,O | — → `WaitlistEntryResponse` (RN33) | ✅ |
| `PATCH /fila/{id}/remover` | A,O | `{reason}` → `WaitlistEntryResponse` | ✅ |
| `GET /notificacoes?status&tipo&destinatario&beneficiarioId` | A,O | → `NotificationResponse[]` | ✅ |
| `GET /notificacoes/contatos-manuais` | A,O | → `NotificationResponse[]` com `waLink` (botão "Lembrar no WhatsApp") | ✅ |
| `PATCH /notificacoes/{id}/concluir` | A,O | — → `NotificationResponse` | ✅ |
| `GET /notificacoes/resumo-diario` | A | → `{date, generatedAt, overdueLoans[], expiredReservations[], openMaintenances[], preventiveDue[]}` (RN49) | ✅ |

#### Dashboard, relatórios, ajuda, assistente e parâmetros
| Método e rota | Perfis | Corpo → Resposta | Status |
|---|---|---|---|
| `GET /dashboard?inicio&fim&tipo` | A,O,V | → `{start, end, equipmentType, generatedAt, personalDataHidden, occupancy{activeEquipments, available, reserved, loaned, inMaintenance, writtenOff, occupancyRate}, overdueLoans, waitlist{waiting, reserved}, maintenanceCost, loanVolume{loans, canceled, returns, openLoans}, byType[], overdueItems[]}` — período padrão 30 dias; Consulta recebe `overdueItems: null` (RN56) | ✅ |
| `GET /relatorios/{tipo}?formato=CSV\|PDF&inicio&fim&tipoEquipamento` | A,O,V | arquivo para download (`Content-Disposition: attachment`). `tipo`: `INVENTARIO` (situação atual, inclui baixados), `EMPRESTIMOS`, `MANUTENCOES`, `ASSISTIDOS` (período padrão 30 dias — RN45). CSV: UTF-8 com BOM, `;`. Consulta: nome/CPF/telefone/endereço mascarados (RN43). Auditado como `EXPORT` (RN44) | ✅ |
| `GET /ajuda/publico?tipo` | **P** | → `HelpContentResponse[]` | ✅ |
| `GET /ajuda?tipo&todos` | A,O,V | → `HelpContentResponse[]` (filtrado pelo perfil; `todos=true` só Admin) | ✅ |
| `POST /ajuda` · `PUT /ajuda/{id}` · `PATCH /ajuda/{id}/inativar` · `/ativar` | A | `{type, title, content, keywords?, audiences[], displayOrder?}` | ✅ |
| `GET /assistente/sugestoes-alocacao` | A | → `{generatedAt, notice, suggestions[], attention[], summary[]}` (só sugere — RN50) | ✅ |
| `GET /assistente/perguntas?q=` | **P** | → `{question, answers[{id, type, title, content, score}], message}` | ✅ |
| `GET /parametros` · `PUT /parametros/{chave}` | A | `{value}` → `SystemParameterResponse` (prazo por tipo saiu daqui: ver `/tipos-equipamento`) | ✅ |
| `GET /dominios` *(proposta)* | **P** | enums + descrições para selects | ❌ |

### 6.3 Fluxos principais (sequência de chamadas)

1. **Pedido público (sem login):** `GET /cep/{cep}` (autopreencher) → `POST /pedidos` → exibir o protocolo.
2. **Análise e empréstimo:** `GET /pedidos?status=PENDENTE_ANALISE` → `GET /equipamentos?status=DISPONIVEL&tipo=...`
   → `POST /pedidos/{id}/aprovar {equipmentId}` → empréstimo `PENDENTE_ASSINATURA` → `GET /emprestimos/{id}/termo`
   (imprimir/mostrar) → `POST /emprestimos/{id}/assinatura-digital` **ou** `.../assinatura-fisica` (upload) → `ATIVO`.
3. **Sem estoque:** `POST /pedidos/{id}/fila` → quando um item do tipo ficar disponível o sistema **reserva por 48 h**
   para o 1º da fila e gera o aviso (contato manual com link wa.me) → aprovar o pedido com o item reservado
   (`POST /pedidos/{id}/aprovar`) ou registrar a recusa (`POST /fila/{id}/recusa`).
4. **Devolução:** `POST /emprestimos/{id}/devolucao` → `BOM_ESTADO`: item volta a DISPONIVEL (e já pode ser reservado
   para a fila) · `PRECISA_MANUTENCAO`/`DANIFICADO`: manutenção corretiva aberta automaticamente e ocorrência de dano
   que bloqueia o beneficiário até ser resolvida (RN40; `DANIFICADO` exige descrição).
5. **Manutenção:** `POST /manutencoes/{id}/custos` → `PUT /manutencoes/{id}` (responsável técnico) →
   `POST /manutencoes/{id}/encerrar {verdict: "APTO"}` (Admin) → item DISPONIVEL.
6. **Etiqueta e leitura:** `GET /equipamentos/{id}/qrcode` (imprimir) → no celular, ler o QR e chamar
   `GET /equipamentos/leitura?codigo=PT-0001` → montar os botões a partir de `availableOperations`.
7. **Sessão:** `POST /auth/login` → guardar `token` + `expiresAt` → `POST /auth/refresh` periódico com atividade →
   em 401 voltar ao login → `POST /auth/logout` ao sair.
