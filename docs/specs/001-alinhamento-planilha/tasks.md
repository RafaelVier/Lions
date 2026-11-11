# Tarefas — Spec 001

## Estratégia de execução

1. **Onda 0 — Fundação (sequencial):** modelo de domínio, persistência, segurança, serviços compartilhados, contratos entre módulos (esqueletos) e handler de erros. Ao final o projeto compila e os testes passam.
2. **Onda 1 — Módulos (paralelo):** 6 frentes independentes (A–F), cada uma em um clone isolado do resultado da Onda 0, com posse exclusiva de arquivos.
3. **Onda 2 — Integração (sequencial):** merge das frentes, resolução de conflitos, suíte completa, atualização da documentação.

### Regras de posse de arquivos (Onda 1)
- Cada frente só **cria/edita** arquivos da sua lista de posse.
- Exceções permitidas (somente **acréscimo**, nunca alteração/remoção de código existente): novos métodos em ports (`domain.repository`), repositórios Spring Data, adapters e métodos de regra em entidades de domínio. Toda exceção deve ser listada no resumo final da frente.
- Contratos da seção 6 do `plan.md` não podem mudar de assinatura.
- Chamadas a serviços de outra frente: somente pelos contratos da seção 6 (esqueletos lançam `UnsupportedOperationException` até a integração — mockar nos testes).

## Onda 0 — Fundação

- [ ] F0.1 `build.gradle.kts`: `com.google.zxing:core:3.5.3` e `com.google.zxing:javase:3.5.3`; `@EnableScheduling` na aplicação.
- [ ] F0.2 Enums do `plan.md §2` (criar/alterar/remover valores).
- [ ] F0.3 Entidades de domínio do `plan.md §3` com regras + testes unitários das máquinas de estado (Equipment/RN12, Loan, WaitlistEntry, Maintenance, Beneficiary/RN05-RN07, LoanOrder, User/RN02).
- [ ] F0.4 Ports, JPA entities, Spring Data repos e adapters do `plan.md §3–§4` (inclui `@Version` e `findByIdForUpdate` em equipamento; remove `Requester*`).
- [ ] F0.5 Serviços compartilhados do `plan.md §5` (Audit, Parameter + controller, CurrentUser, FileStorage, LgpdMasker, CpfValidator, PasswordPolicy, WhatsAppMessageHelper genérico) + `Clock` bean.
- [ ] F0.6 Eventos e esqueletos/contratos do `plan.md §6` + DTOs dos contratos.
- [ ] F0.7 `ConflictException` + `GlobalExceptionHandler` (`presentation.handler`) conforme `plan.md §1`.
- [ ] F0.8 Segurança do `plan.md §9` (roles, rotas públicas, JWT 30 min com `jti`, `TokenRevocationService`, entry point 401/403 JSON, admin inicial configurável, CORS por propriedade, seed só em `dev`).
- [ ] F0.9 Ajustar código existente para compilar com o novo modelo (serviços antigos reduzidos ao mínimo; donos da Onda 1 reescrevem).
- [ ] F0.10 `src/test/resources/application.yml` com `lions.scheduling.enabled=false`; `./gradlew test` verde.

## Onda 1 — Frentes paralelas

### Frente A — Acesso, ajuda e assistente (RF01, RF13, RF14)
Posse: `application/service/auth/**`, `application/service/user/**`, `application/service/help/**`, `application/service/assistant/**`, `presentation/controller/{Auth,User,Help,Assistant}Controller.java`, DTOs `Auth*`, `Login*`, `User*`, `Password*`, `Help*`, `Assistant*`, remoção de `RegisterRequest`, testes correspondentes.
- [ ] A.1 Login/refresh/logout/troca de senha (RN57) com auditoria.
- [ ] A.2 CRUD de usuários, perfil, inativação, redefinição de senha (RN01–RN03).
- [ ] A.3 Conteúdo de ajuda (RN53) + FAQ pública + seed `dev`.
- [ ] A.4 Assistente por regras: sugestões de alocação e busca de FAQ (RN50, RN51).

### Frente B — Beneficiários, solicitações e histórico do assistido (RF02, RF09, pedido público)
Posse: `application/service/beneficiary/**`, `application/service/order/**`, `presentation/controller/{Beneficiary,LoanOrder,Cep}Controller.java`, DTOs `Beneficiary*`, `LoanOrder*`, `Consent*`, `Cep*` (application), remoção de `LoanApprovalRequest` se não usado, testes correspondentes.
- [ ] B.1 CRUD + consentimento + inativação/reativação + anonimização (RN03–RN08, RN34, RN59).
- [ ] B.2 Histórico consolidado (RN42).
- [ ] B.3 Pedido público + aprovação (→ `LoanService.registerLoan`) + rejeição + envio à fila (→ `WaitlistService.enqueue`).
- [ ] B.4 Consulta CEP.

### Frente C — Equipamentos, QR Code e manutenção (RF03, RF04, RF08)
Posse: `application/service/equipment/**`, `application/service/maintenance/**`, `infrastructure/qrcode/**`, `infrastructure/scheduler/PreventiveMaintenanceJob.java`, `presentation/controller/{Equipment,Maintenance}Controller.java`, DTOs `Equipment*`, `Maintenance*`, `WriteOff*`, `QrCode*`, `Preventive*`, testes correspondentes.
- [ ] C.1 Cadastro com patrimônio gerado, edição restrita, listagem/filtros, baixa (RN09, RN11–RN14).
- [ ] C.2 QR Code PNG e leitura com operações disponíveis (RN10, RN15).
- [ ] C.3 Manutenção: abertura, custos/ajustes, encerramento com parecer, listener de avaria (RN24, RN35–RN38, RN41).
- [ ] C.4 Preventiva: `PreventiveMaintenanceService.findDue` + job (RN39).
- [ ] C.5 Histórico do equipamento.

### Frente D — Empréstimos, termo, ocorrências e auditoria (RF05, RF07, RF10)
Posse: `application/service/loan/**` (inclui `LoanService`, `BeneficiaryRestrictionService`, `PdfTermService`, `OccurrenceService`), `infrastructure/scheduler/OverdueLoanJob.java`, `presentation/controller/{Loan,Occurrence,Audit}Controller.java`, DTOs `Loan*` (exceto contratos — só acréscimo), `Signature*`, `Renewal*`, `Return*`, `Occurrence*`, `Audit*`, `Adjustment*`, remoção de `TermAcceptanceRequest`/`LoanReturnRequest` antigos se substituídos, testes correspondentes.
- [ ] D.1 Registro de empréstimo (RN16–RN22, RN28) + termo PDF armazenado (RN06, RN21, RN61).
- [ ] D.2 Assinatura digital e física (RN21, RN25).
- [ ] D.3 Renovação (RN23), devolução com vistoria + ocorrência + eventos (RN24, RN40), cancelamento (RN26), ajustes (RN41).
- [ ] D.4 `BeneficiaryRestrictionService` (RN08, RN20).
- [ ] D.5 Job de atraso/inadimplência (RN27) + ocorrências (listar/resolver).
- [ ] D.6 Consulta de auditoria (RN58) e histórico de empréstimos com filtros (RF10).

### Frente E — Fila de espera e notificações (RF06, RF12)
Posse: `application/service/waitlist/**`, `application/service/notification/**`, `infrastructure/notification/**`, `infrastructure/scheduler/{ReservationExpiration,DueDateReminder,NotificationRetry,DailySummary}Job.java`, `presentation/controller/{Waitlist,Notification}Controller.java`, DTOs `Waitlist*`, `Priority*`, `Notification*`, `DailySummary*`, templates em `WhatsAppMessageHelper` (só acréscimo), testes correspondentes.
- [ ] E.1 Entrada na fila, ordenação, prioridade, recusa, remoção (RN29–RN31, RN33, RN34).
- [ ] E.2 Reserva automática por evento + expiração (RN32, RN33).
- [ ] E.3 Envio por canal autorizado, porta de envio + adaptador de log/wa.me, retentativas e contato manual (RN47, RN48).
- [ ] E.4 Régua de lembretes (RN46) e resumo diário (RN49).

### Frente F — Dashboard e relatórios (RF15, RF11)
Posse: `application/service/dashboard/**`, `application/service/report/**`, `presentation/controller/{Dashboard,Report}Controller.java`, DTOs `Dashboard*`, `Report*`, remoção de `DashboardIndicatorsResponse`, testes correspondentes.
- [ ] F.1 Indicadores com filtros e visão por perfil (RN54–RN56).
- [ ] F.2 Relatórios CSV/PDF, mascaramento por perfil, período padrão e auditoria de exportação (RN43–RN45, RN55).

## Onda 2 — Integração
- [ ] I.1 Merge das frentes; resolver conflitos em ports/adapters.
- [ ] I.2 Teste de integração ponta a ponta: pedido → aprovação → assinatura → devolução com avaria → manutenção → APTO → reserva para a fila.
- [ ] I.3 `./gradlew test` verde.
- [ ] I.4 Atualizar `README.md`, `backend/README.md` e `docs/DESCRICAO_DO_PROJETO_E_BACKEND.md`.

## Definição de pronto (por frente)
- Compila, `./gradlew test` verde no clone.
- Cada RN da frente com teste automatizado.
- Endpoints documentados no Swagger (`@Tag`, `@Operation`).
- Sem acesso direto a `LocalDate.now()`/`LocalDateTime.now()` fora do `Clock`.
- Resumo final: arquivos criados/alterados, exceções de posse e pendências.
