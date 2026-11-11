# Plano técnico — Spec 001

## 1. Convenções gerais

- Pacote raiz `org.lions.backend`. Camadas:
  - `domain.entity` (POJOs com Lombok, **sem JPA**, com regras), `domain.enums`, `domain.repository` (ports), `domain.exception`.
  - `application.service.<modulo>` (casos de uso, `@Transactional`), `application.dto.request|response`, `application.event` (eventos), `application.port` (portas não-repositório), `application.support` (utilitários: máscara LGPD, CPF, senha).
  - `infrastructure.persistence.{entity,repository,adapter}`, `infrastructure.security`, `infrastructure.storage`, `infrastructure.notification`, `infrastructure.qrcode`, `infrastructure.scheduler`, `infrastructure.client`, `infrastructure.config`.
  - `presentation.controller` (REST), `presentation.handler` (`GlobalExceptionHandler`).
- Identificadores de código e campos JSON em **inglês camelCase**; rotas e mensagens em **português**.
- Rotas sob `/api/v1`. Permissões via `@PreAuthorize("hasRole('ADMIN')")` / `hasAnyRole('ADMIN','OPERATOR')` / `hasAnyRole('ADMIN','OPERATOR','VIEWER')`.
- Todo método público de serviço é `@Transactional` (`readOnly = true` em consultas) — schedulers não têm OSIV.
- Datas/horas sempre via bean `java.time.Clock` (injetado) — nunca `LocalDate.now()` sem clock (testabilidade).
- Usuário corrente via `CurrentUserPort.username()` (retorna `"SISTEMA"` sem autenticação) e `CurrentUserPort.hasRole(UserRole)`.
- Adapters: associações mapeadas com `jpaRepository.getReferenceById(id)` (ou `entityManager.getReference`) em vez de reconstruir o grafo; `toDomain` pode mapear o grafo necessário.
- Toda alteração de estado relevante chama `AuditService` (RN25/RN52/RN58).
- Erros (`ProblemDetail`, RFC 7807):
  | Exceção | HTTP |
  |---|---|
  | `MethodArgumentNotValidException`, `ConstraintViolationException`, `HttpMessageNotReadableException`, `MissingServletRequestParameterException` | 400 (`errors`: campo→mensagem) |
  | `UnauthorizedException`, `AuthenticationException` | 401 |
  | `AccessDeniedException` | 403 |
  | `ResourceNotFoundException` | 404 |
  | `ConflictException` (nova; `Map<String,Object> details`), `ObjectOptimisticLockingFailureException`, `PessimisticLockingFailureException` | 409 |
  | `BusinessException` | 422 |
  | demais | 500 (mensagem genérica, log do stack) |

## 2. Enums (`domain.enums`)

| Enum | Valores |
|---|---|
| `UserRole` | `ROLE_ADMIN`, `ROLE_OPERATOR`, `ROLE_VIEWER` |
| `EquipmentType` | `CADEIRA_DE_RODAS`, `CADEIRA_DE_BANHO`, `ANDADOR`, `MULETAS`, `OUTROS` (com `description`) |
| `EquipmentStatus` | `DISPONIVEL`, `RESERVADO`, `EMPRESTADO`, `EM_MANUTENCAO`, `BAIXADO` + `boolean canTransitionTo(EquipmentStatus)` (RN12) |
| `EquipmentCondition` | `BOM`, `REGULAR`, `RUIM` |
| `WriteOffReason` | `INSERVIVEL`, `PERDA`, `FURTO`, `DOACAO` |
| `DocumentType` | `CPF`, `ESTRANGEIRO`, `CNPJ` |
| `NotificationChannel` | `WHATSAPP`, `SMS`, `EMAIL` |
| `OrderStatus` | `PENDENTE_ANALISE`, `APROVADO`, `REJEITADO`, `EM_FILA_ESPERA`, `CANCELADO` |
| `LoanStatus` | `PENDENTE_ASSINATURA`, `ATIVO`, `ATRASADO`, `DEVOLVIDO`, `CANCELADO` (+ `isOpen()` = PENDENTE_ASSINATURA/ATIVO/ATRASADO) |
| `SignatureType` | `FISICA_DIGITALIZADA`, `DIGITAL` |
| `ReturnCondition` | `BOM_ESTADO`, `PRECISA_MANUTENCAO`, `DANIFICADO` |
| `OccurrenceType` | `DANO`, `INADIMPLENCIA` |
| `OccurrenceStatus` | `ABERTA`, `RESOLVIDA` |
| `WaitlistStatus` | `AGUARDANDO`, `RESERVADO`, `ATENDIDO`, `REMOVIDO` |
| `PriorityReason` | `URGENCIA_SAUDE`, `PESSOA_IDOSA`, `POS_OPERATORIO` |
| `MaintenanceType` | `CORRETIVA`, `PREVENTIVA` |
| `MaintenanceStatus` | `EM_ANDAMENTO`, `CONCLUIDA_APTO`, `CONCLUIDA_INSERVIVEL` |
| `MaintenanceVerdict` | `APTO`, `INSERVIVEL` |
| `CostType` | `PECA`, `SERVICO` |
| `NotificationType` | `LEMBRETE_7_DIAS`, `LEMBRETE_1_DIA`, `VENCIMENTO_HOJE`, `ATRASO`, `RESERVA_DISPONIVEL`, `INADIMPLENCIA`, `RESUMO_DIARIO`, `CONTATO_MANUAL`, `PREVENTIVA_VENCIDA`, `GERAL` |
| `NotificationStatus` | `PENDENTE`, `ENVIADA`, `FALHA`, `AGUARDANDO_CONTATO_MANUAL`, `CONCLUIDA_MANUALMENTE` |
| `RecipientType` | `BENEFICIARIO`, `ADMINISTRADOR` |
| `AuditAction` | `CREATE`, `UPDATE`, `INACTIVATE`, `REACTIVATE`, `STATUS_CHANGE`, `CANCEL`, `ANONYMIZE`, `ADJUSTMENT`, `LOGIN`, `LOGOUT`, `EXPORT` |
| `HelpContentType` | `TUTORIAL`, `DICA`, `FAQ`, `ONBOARDING` |
| `HelpAudience` | `PUBLICO`, `ADMIN`, `OPERADOR`, `CONSULTA` |
| `ReportType` | `INVENTARIO`, `EMPRESTIMOS`, `ASSISTIDOS`, `MANUTENCOES` |
| `ReportFormat` | `PDF`, `CSV` |

Removidos: `UserRole.ROLE_MANAGER/ROLE_MEMBER`, `EquipmentCondition.NOVO/SEMINOVO/USADO/EM_MANUTENCAO/BAIXADO`, `LoanStatus.RENOVADO`, `OrderStatus.PENDENTE_ESTOQUE`, `MaintenanceStatus.CONCLUIDA/IRRECUPERAVEL`.

## 3. Modelo de domínio (`domain.entity`) e tabelas

`(opc.)` = nullable, revisar com o MER. Todas têm `id`, `createdAt`, `updatedAt` (exceto onde indicado), auditados via `@CreatedDate/@LastModifiedDate`.

### User → `tb_users`
`username` (único), `fullName (opc.)`, `email` (único), `password` (BCrypt), `role`, `active`, `lastLoginAt (opc.)`.
Regras: `deactivate()`, `activate()`, `changeRole(newRole, actingUsername)` → `BusinessException` se `actingUsername == username` (RN02).

### Beneficiary → `tb_beneficiaries` (substitui `Requester`/`tb_requesters`)
`fullName`, `documentType` (default CPF), `documentNumber` (só dígitos p/ CPF/CNPJ), `birthDate`, `phone`, `email (opc.)`,
`cep`, `street`, `number`, `complement (opc.)`, `neighborhood`, `city`, `state`,
`alternativeContactName`, `alternativeContactPhone`, `alternativeContactRelationship (opc.)`,
`legallyIncapable` (bool), `legalGuardianName (opc.)`, `legalGuardianDocument (opc.)`, `legalGuardianPhone (opc.)`, `legalGuardianRelationship (opc.)`,
`consentGranted`, `consentTimestamp`, `consentGivenBy (opc.)`, `notificationChannels` (`Set<NotificationChannel>`, `@ElementCollection` em `tb_beneficiary_channels`),
`active`, `inactivatedAt (opc.)`, `inactivationReason (opc.)`, `anonymized`, `anonymizedAt (opc.)`, `notes (opc.)`.
Regras: `validate(LocalDate today)` (RN05, RN06, RN07, CPF válido via `CpfValidator`), `isMinor(today)`, `requiresLegalGuardian(today)`, `signatoryName(today)` / `signatoryDocument(today)`, `inactivate(reason, now)`, `reactivate(now)`, `anonymize(now)`, `getMaskedDocument()`.
Índices: `documentNumber`, `active`.

### LoanOrder (Solicitação) → `tb_loan_orders`
`beneficiary` (FK), `forSelf`, `requesterName (opc.)`, `requesterRelationship (opc.)`, `equipmentType`, `needReason (opc., dado sensível)`, `status`, `rejectionReason (opc.)`, `assistedRegistration`, `decidedBy (opc.)`, `decidedAt (opc.)`.
Regras: `approve(by, now)`, `reject(reason, by, now)`, `sendToWaitlist(by, now)`, `cancel(...)` — só a partir de `PENDENTE_ANALISE` (e `EM_FILA_ESPERA` → `APROVADO` permitido).

### Equipment → `tb_equipments`
`patrimonyNumber` (único, gerado `PT-%04d` a partir do id após o primeiro save), `type`, `size (opc.)`, `condition`, `status`, `notes (opc.)`, `photoUrl (opc.)`,
`reservedForBeneficiaryId (opc.)`, `writeOffReason (opc.)`, `writeOffNotes (opc.)`, `writeOffDate (opc.)`, `writeOffBy (opc.)`,
`lastMaintenanceDate (opc.)`, `loanCyclesSinceMaintenance` (int), `version` (`@Version`, Long).
Regras (todas validam RN12 via `EquipmentStatus.canTransitionTo`, lançando `BusinessException`):
`reserveFor(beneficiaryId)`, `releaseReservation()`, `markAsLoaned(beneficiaryId)` (RN17: DISPONIVEL, ou RESERVADO para o mesmo beneficiário), `markAsReturned(boolean needsMaintenance)` (incrementa ciclos), `sendToMaintenance()`, `returnFromMaintenance(LocalDate)` (zera ciclos, seta `lastMaintenanceDate`), `writeOff(reason, notes, by, today)`, `isAvailable()`, `isActive()` (≠ BAIXADO).
Índices: `status`, `type`, `(type,status)`.

### Loan → `tb_loans`
`beneficiary` (FK), `equipment` (FK), `order (opc., FK)`, `status`, `loanDate`, `initialExpectedReturnDate`, `expectedReturnDate`, `renewalCount`, `lastRenewalDate (opc.)`,
`retroactive` (bool), `retroactiveJustification (opc.)`, `initialCondition`, `deliveryNotes (opc.)`, `deliveryPhotoUrl (opc.)`,
`termPdfPath`, `termHashSha256`, `termGeneratedAt`, `signatureType (opc.)`, `signedAt (opc.)`, `signedByName (opc.)`, `signedDocumentPath (opc.)`, `signatureRegisteredBy (opc.)`,
`createdBy`, `returnedAt (opc.)`, `cancelReason (opc.)`, `canceledBy (opc.)`, `canceledAt (opc.)`.
Regras: `registerSignature(type, signerName, documentPath, by, now)` (PENDENTE_ASSINATURA→ATIVO), `canRenew(...)`/`renew(newDueDate, today)`, `markOverdue(today)` (ATIVO→ATRASADO se `today > expectedReturnDate`), `isOverdue(today)`, `daysOverdue(today)`, `markReturned(now)`, `cancel(reason, by, now)`.
Índices: `status`, `beneficiary_id`, `equipment_id`, `expected_return_date`.

### LoanRenewal → `tb_loan_renewals` (imutável)
`loanId`, `previousDueDate`, `newDueDate`, `renewedBy`, `renewedAt`.

### LoanReturn → `tb_loan_returns` (imutável)
`loan` (FK, único), `returnDate`, `returnCondition`, `damageDescription (opc.)`, `returnPhotoUrl (opc.)`, `hasDamage`, `receivedBy`.

### Occurrence → `tb_occurrences`
`beneficiary` (FK), `loanId (opc.)`, `type`, `status`, `description`, `automatic`, `createdBy`, `resolvedAt (opc.)`, `resolvedBy (opc.)`, `resolutionNote (opc.)`.
Regras: `resolve(note, by, now)`.

### WaitlistEntry → `tb_waitlist_entries`
`beneficiary` (FK), `equipmentType`, `orderId (opc.)`, `status`, `queuedAt` (ordenação FIFO; reinicia ao voltar ao fim), `originalRequestedAt`,
`priority` (bool), `priorityReason (opc.)`, `priorityJustification (opc.)`, `priorityAuthorizedBy (opc.)`,
`reservedEquipmentId (opc.)`, `reservedAt (opc.)`, `reservationExpiresAt (opc.)`, `consecutiveExpirations` (int), `removedReason (opc.)`, `closedAt (opc.)`, `createdBy`.
Regras: `reserve(equipmentId, now, hours)`, `expireOrDecline(now, maxConsecutive)` (volta ao fim ou `REMOVIDO`), `serve(now)`, `remove(reason, now)`, `setPriority(reason, justification, by)`, `clearPriority()`.
Índices: `(equipment_type,status)`, `beneficiary_id`.

### Maintenance → `tb_maintenances` / MaintenanceCost → `tb_maintenance_costs`
Maintenance: `equipment` (FK), `type`, `status`, `entryDate`, `exitDate (opc.)`, `defectDescription`, `technicalResponsible`, `verdictNotes (opc.)`, `originLoanId (opc.)`, `openedBy`, `closedBy (opc.)`, `costs` (`List<MaintenanceCost>`).
MaintenanceCost: `maintenanceId`, `type`, `description`, `partReplaced (opc.)`, `amount` (BigDecimal ≥ 0; ajuste pode ser negativo), `adjustment` (bool), `adjustmentReason (opc.)`, `registeredBy`, `registeredAt`.
Regras: `validateOpening()` (RN36), `addCost(...)` (só EM_ANDAMENTO, salvo ajuste), `close(verdict, notes, by, today)`, `totalCost()`.

### Notification → `tb_notifications`
`recipientType`, `beneficiaryId (opc.)`, `loanId (opc.)`, `channel (opc.; null = in-app/manual)`, `type`, `message` (TEXT), `status`, `attempts`, `lastError (opc.)`, `waLink (opc.)`, `referenceDate (opc.)` (dedupe RN46), `sentAt (opc.)`, `resolvedBy (opc.)`, `resolvedAt (opc.)`.

### AuditLog → `tb_audit_logs` (sem `updatedAt`)
`entityName`, `entityId (opc.)`, `action`, `username`, `automatic`, `originRule (opc.)`, `details` (TEXT/JSON: alterações `campo: {old,new}`, filtros etc.), `occurredAt`.

### SystemParameter → `tb_system_parameters`
`key` (PK), `value`, `description (opc.)`, `updatedBy (opc.)`, `updatedAt`.

### HelpContent → `tb_help_contents`
`type`, `title`, `content` (TEXT), `keywords (opc.)`, `audiences` (`Set<HelpAudience>`, `@ElementCollection`), `displayOrder`, `active`.

## 4. Ports (`domain.repository`)

Cada agregado tem `XxxRepositoryPort` com `save`, `findById`, `findAll` e as consultas abaixo (adapter + Spring Data correspondentes):

- `UserRepositoryPort`: `findByUsername`, `findByEmail`, `existsByUsername`, `existsByEmail`, `findAll`, `countByRoleAndActiveTrue(UserRole)`.
- `BeneficiaryRepositoryPort`: `findActiveByDocumentNumber`, `searchByName(String)`, `findAll(Boolean active)`, `count`.
- `LoanOrderRepositoryPort`: `findByStatus`, `findByBeneficiaryId`, `countByStatus`.
- `EquipmentRepositoryPort`: `findByIdForUpdate` (lock pessimista), `findByPatrimonyNumber`, `findByStatus`, `findByType`, `findByTypeAndStatus`, `countByStatus`, `countByTypeAndStatus`, `countTotal`, `findAllActive`.
- `LoanRepositoryPort`: `findByStatus`, `findByStatusIn`, `findByBeneficiaryId`, `findByEquipmentId`, `findOpenByBeneficiaryId`, `findOpenDueBetween(start,end)`, `findOpenOverdue(today)`, `findByLoanDateBetween(start,end)`, `countByStatus`, `countByLoanDateBetween`.
- `LoanRenewalRepositoryPort`: `findByLoanId`.
- `LoanReturnRepositoryPort`: `findByLoanId`, `findByReturnDateBetween`.
- `OccurrenceRepositoryPort`: `findByBeneficiaryId`, `findByStatus`, `existsByBeneficiaryIdAndStatus`, `existsByLoanIdAndType`.
- `WaitlistEntryRepositoryPort`: `findActiveByType(type)` (AGUARDANDO ordenado: `priority desc, queuedAt asc`), `findByStatus`, `findByBeneficiaryId`, `findActiveByBeneficiaryId`, `existsActiveByBeneficiaryIdAndType`, `findExpiredReservations(now)`, `countActiveByType`.
- `MaintenanceRepositoryPort`: `findByEquipmentId`, `findByStatus`, `findByEntryDateBetween`, `findLastClosedByEquipmentId`.
- `NotificationRepositoryPort`: `findByStatus`, `findRetryable(maxAttempts)`, `existsByLoanIdAndTypeAndReferenceDate`, `findAll(filters)`.
- `AuditLogRepositoryPort`: `save`, `search(entityName, entityId, from, to)`.
- `SystemParameterRepositoryPort`: `findByKey`, `findAll`, `save`.
- `HelpContentRepositoryPort`: `findActiveByAudience(HelpAudience)`, `findAll`, `findById`, `save`.

## 5. Serviços compartilhados (Onda 0, prontos para uso)

- `AuditService.record(entityName, entityId, action, details)` (usuário do `CurrentUserPort`), `recordAutomatic(entityName, entityId, action, originRule, details)`, `diff(Object before, Object after, String... fields)` → `Map` para RN58, `search(...)`.
- `ParameterService` (+ `ParameterController`: `GET /api/v1/parametros` ADMIN, `PUT /api/v1/parametros/{chave}` ADMIN) com getters tipados e defaults:

| Chave | Default |
|---|---|
| `loan.default-days` | 30 |
| `loan.days.<TIPO>` | (usa default) |
| `loan.max-active-per-beneficiary` | 2 |
| `loan.max-renewals` | 2 |
| `loan.renewal-window-days` | 7 |
| `loan.default-escalation-days` | 30 |
| `waitlist.reservation-hours` | 48 |
| `waitlist.max-consecutive-expirations` | 2 |
| `maintenance.preventive-months` | 12 |
| `maintenance.preventive-loan-cycles` | 10 |
| `notification.max-attempts` | 3 |
| `notification.overdue-interval-days` | 3 |
| `report.default-period-days` | 30 |

- `CurrentUserPort` (`application.port`) + `SecurityCurrentUserAdapter`.
- `FileStoragePort.store(String folder, String fileName, byte[] content) → String path`, `read(String path) → byte[]` + `LocalFileStorageAdapter` (`lions.storage.base-dir`, default `./data/storage`).
- `LgpdMasker` (`application.support`): `maskDocument`, `maskName`, `maskPhone`, `maskAddress`.
- `CpfValidator`, `PasswordPolicy` (`application.support`).
- `WhatsAppMessageHelper` (genérico: `buildLink(phone, message)` + templates sem "90 dias").

## 6. Contratos entre módulos

### 6.1 Eventos (`application.event`, records, publicados **após** persistir, consumidos com `@EventListener` síncrono na mesma transação)
| Evento | Publicado por | Consumido por |
|---|---|---|
| `EquipmentAvailableEvent(Long equipmentId, EquipmentType type)` | Equipamentos (criação), Manutenção (APTO), Empréstimos (devolução BOM_ESTADO, cancelamento), Fila (liberação de reserva) | Fila → reserva para o 1º (RN32) |
| `EquipmentDamagedOnReturnEvent(Long equipmentId, Long loanId, Long beneficiaryId, String description)` | Empréstimos (devolução com avaria) | Manutenção → abre corretiva (RN24) |
| `BeneficiaryRestrictedEvent(Long beneficiaryId, String reason)` | Beneficiários (inativação/anonimização), Empréstimos (atraso, ocorrência) | Fila → remove posições ativas (RN34) |

### 6.2 Serviços com assinatura fixa (esqueleto criado na Onda 0; corpo implementado pelo dono)
```java
// Dono: Empréstimos (D)
LoanService.registerLoan(LoanCreateRequest req) : LoanResponse
BeneficiaryRestrictionService.evaluate(Long beneficiaryId) : BeneficiaryRestriction
//   record BeneficiaryRestriction(boolean blocked, boolean hasOpenLoan, boolean hasOpenOccurrence, List<String> reasons)
// Dono: Fila + Notificações (E)
WaitlistService.enqueue(WaitlistEnqueueRequest req) : WaitlistEntryResponse
WaitlistService.hasWaitingFor(EquipmentType type) : boolean
WaitlistService.hasActiveEntry(Long beneficiaryId) : boolean
WaitlistService.markServed(Long beneficiaryId, Long equipmentId, EquipmentType type) : void
NotificationService.notifyBeneficiary(Long beneficiaryId, Long loanId, NotificationType type, String message) : void
NotificationService.notifyAdmins(NotificationType type, String message) : void
// Dono: Equipamentos + Manutenção (C)
PreventiveMaintenanceService.findDue() : List<PreventiveMaintenanceDue>
//   record PreventiveMaintenanceDue(Long equipmentId, String patrimonyNumber, EquipmentType type, LocalDate lastMaintenanceDate, int loanCycles, String reason)
```
DTOs `LoanCreateRequest` (`beneficiaryId`*, `equipmentId`*, `orderId`, `loanDate`, `retroactiveJustification`, `deliveryNotes`, `deliveryPhotoUrl`), `LoanResponse`, `WaitlistEnqueueRequest` (`beneficiaryId`*, `equipmentType`*, `orderId`), `WaitlistEntryResponse` são criados na Onda 0; donos podem **acrescentar** campos.

## 7. Endpoints

Legenda de perfis: **A** = ADMIN, **O** = OPERATOR, **V** = VIEWER, **P** = público.

### Auth / usuários (RF01)
| Método e rota | Perfis | RN |
|---|---|---|
| `POST /auth/login` | P | RN57 (audita LOGIN, `lastLoginAt`) |
| `POST /auth/refresh` | A,O,V | RN57 |
| `POST /auth/logout` | A,O,V | RN57 (revoga `jti`, audita LOGOUT) |
| `PUT /auth/senha` | A,O,V | RN57 (senha atual + nova forte) |
| `GET /usuarios`, `GET /usuarios/{id}` | A | |
| `POST /usuarios` | A | RN02, RN57 |
| `PUT /usuarios/{id}` | A | RN02, RN58 (nome/e-mail) |
| `PATCH /usuarios/{id}/perfil` | A | RN02 (não o próprio) |
| `PATCH /usuarios/{id}/inativar`, `/ativar` | A | RN03 (não a si; manter ≥ 1 admin ativo) |
| `PATCH /usuarios/{id}/senha` | A | redefinição pelo Admin |

### Beneficiários e solicitações (RF02, RF09)
| Método e rota | Perfis | RN |
|---|---|---|
| `POST /beneficiarios` | A,O | RN04–RN07 |
| `GET /beneficiarios?nome&documento&ativo` | A,O | documento mascarado na lista |
| `GET /beneficiarios/{id}` | A,O | |
| `PUT /beneficiarios/{id}` | A,O | RN58, RN59 (correção) |
| `PUT /beneficiarios/{id}/consentimento` | A,O | RN07, RN47 |
| `PATCH /beneficiarios/{id}/inativar` `{motivo}` | A | RN08, RN34 |
| `PATCH /beneficiarios/{id}/reativar` | A | RN04 (não pode haver outro ativo com o doc) |
| `POST /beneficiarios/{id}/anonimizar` | A | RN59 |
| `GET /beneficiarios/{id}/historico` | A,O | RN42 |
| `POST /pedidos` | P | cria/vincula beneficiário (RN04/05/07), status PENDENTE_ANALISE |
| `GET /pedidos?status`, `GET /pedidos/{id}` | A,O | |
| `POST /pedidos/{id}/aprovar` `{equipmentId, loanDate?, ...}` | A,O | chama `LoanService.registerLoan` |
| `POST /pedidos/{id}/rejeitar` `{motivo}` | A,O | |
| `POST /pedidos/{id}/fila` | A,O | chama `WaitlistService.enqueue`, status EM_FILA_ESPERA |
| `GET /cep/{cep}` | P | ViaCEP |

### Equipamentos, QR e manutenção (RF03, RF04, RF08)
| Método e rota | Perfis | RN |
|---|---|---|
| `POST /equipamentos` | A,O | RN09, RN14; publica `EquipmentAvailableEvent` |
| `GET /equipamentos?status&tipo&condicao` | A,O,V | |
| `GET /equipamentos/{id}`, `GET /equipamentos/patrimonio/{numero}` | A,O,V | |
| `PUT /equipamentos/{id}` | A,O | RN09 (patrimônio/status não editáveis), RN58 |
| `POST /equipamentos/{id}/baixa` `{reason, notes}` | A | RN13 |
| `GET /equipamentos/{id}/qrcode?tamanho=300` | A,O | RN10 (PNG) |
| `GET /equipamentos/leitura?codigo=` | A,O | RN15 (`availableOperations`) |
| `GET /equipamentos/{id}/historico` | A,O | empréstimos + manutenções + custo acumulado (RN38) |
| `POST /manutencoes` | A,O | RN36 |
| `GET /manutencoes?status&equipamentoId`, `GET /manutencoes/{id}` | A,O | |
| `POST /manutencoes/{id}/custos` | A,O (ajuste só A) | RN38, RN41 |
| `POST /manutencoes/{id}/encerrar` `{verdict, notes}` | A,O (INSERVIVEL só A) | RN37 |
| `GET /manutencoes/preventivas-pendentes` | A,O | RN39 |

### Empréstimos, termo, ocorrências e auditoria (RF05, RF07, RF10)
| Método e rota | Perfis | RN |
|---|---|---|
| `POST /emprestimos` | A,O (retroativo só A) | RN17–RN22, RN28 |
| `GET /emprestimos?status&beneficiarioId&equipamentoId&inicio&fim` | A,O | |
| `GET /emprestimos/{id}` | A,O | inclui renovações e devolução |
| `GET /emprestimos/{id}/termo` | A,O | RN61 (PDF armazenado) |
| `POST /emprestimos/{id}/assinatura-digital` `{signerName}` | A,O | RN06, RN21 |
| `POST /emprestimos/{id}/assinatura-fisica` (multipart `file`, `signerName`) | A,O | RN21 |
| `POST /emprestimos/{id}/renovacao` | A,O | RN23 |
| `POST /emprestimos/{id}/devolucao` | A,O | RN24, RN40 |
| `POST /emprestimos/{id}/cancelamento` `{motivo}` | A | RN26 |
| `POST /emprestimos/{id}/ajustes` `{descricao, motivo}` | A | RN41 |
| `GET /ocorrencias?beneficiarioId&status` | A,O | |
| `PATCH /ocorrencias/{id}/resolver` `{nota}` | A | RN20 |
| `GET /auditoria?entidade&entidadeId&inicio&fim` | A | RN58 |

### Fila e notificações (RF06, RF12)
| Método e rota | Perfis | RN |
|---|---|---|
| `POST /fila` | A,O | RN29, RN31, RN34 |
| `GET /fila?tipo&status` | A,O | ordem efetiva |
| `PATCH /fila/{id}/prioridade` `{reason, justification}` / `DELETE /fila/{id}/prioridade` | A | RN30 |
| `POST /fila/{id}/recusa` | A,O | RN33 |
| `PATCH /fila/{id}/remover` `{motivo}` | A,O | |
| `GET /notificacoes?status&tipo` | A,O | |
| `GET /notificacoes/contatos-manuais` | A,O | RN47, RN48 |
| `PATCH /notificacoes/{id}/concluir` | A,O | |
| `GET /notificacoes/resumo-diario` | A | RN49 |

### Dashboard e relatórios (RF15, RF11)
| Método e rota | Perfis | RN |
|---|---|---|
| `GET /dashboard?inicio&fim&tipo` | A,O,V | RN54–RN56 |
| `GET /relatorios/{tipo}?formato&inicio&fim&tipoEquipamento` | A,O,V | RN43–RN45, RN55 |

### Ajuda e assistente (RF14, RF13)
| Método e rota | Perfis | RN |
|---|---|---|
| `GET /ajuda/publico` | P | RN53 (audiência PUBLICO) |
| `GET /ajuda?tipo` | A,O,V | filtrado pelo perfil |
| `POST /ajuda`, `PUT /ajuda/{id}`, `PATCH /ajuda/{id}/inativar` | A | RN53 |
| `GET /assistente/sugestoes-alocacao` | A | RN50, RN51 |
| `GET /assistente/perguntas?q=` | P | RN51 (só FAQ PUBLICO quando anônimo) |

## 8. Rotinas agendadas (`infrastructure.scheduler`, habilitadas por `lions.scheduling.enabled`, default `true`; `false` nos testes)
| Job | Cron | Dono | RN |
|---|---|---|---|
| `OverdueLoanJob` | `0 0 1 * * *` | D | RN27, RN20, RN34 |
| `ReservationExpirationJob` | `0 0 * * * *` | E | RN33 |
| `DueDateReminderJob` | `0 0 8 * * *` | E | RN46, RN47 |
| `NotificationRetryJob` | `0 */15 * * * *` | E | RN48 |
| `DailySummaryJob` | `0 0 7 * * *` | E | RN49 |
| `PreventiveMaintenanceJob` | `0 30 1 * * *` | C | RN39 (notifica Admin uma vez por item/mês) |

## 9. Segurança
- JWT HS256 com `jti`; `lions.security.jwt.expiration-ms: 1800000` (30 min); segredo via `LIONS_JWT_SECRET` (default só dev).
- `TokenRevocationService` em memória (`jti` → expiração) consultado pelo filtro.
- Públicos: `POST /auth/login`, `POST /pedidos`, `GET /cep/**`, `GET /ajuda/publico`, `GET /assistente/perguntas`, Swagger, `/h2-console/**` (só dev), `/actuator/health`.
- Removidos: `/auth/register`, `GET /pedidos/{id}/termo` e `POST /pedidos/{id}/aceite-termo` públicos (expunham dados pessoais por id sequencial).
- Admin inicial configurável: `lions.security.default-admin.username|password` (default `marcio` / `Lions@123`, senha forte).
- CORS por `lions.cors.allowed-origins` (default `*`).
- Seed de equipamentos só no perfil `dev`.

## 10. Testes
- Domínio: testes unitários puros das regras (JUnit 5).
- Serviços: Mockito, `Clock` fixo.
- API: `@SpringBootTest` + `@AutoConfigureMockMvc` + `@WithMockUser(roles=...)`, H2, `lions.scheduling.enabled=false` (`src/test/resources/application.yml` ou `@TestPropertySource`).
- Cada RN da `spec.md` coberta por ao menos um teste; nomes `deve...Quando...`.
