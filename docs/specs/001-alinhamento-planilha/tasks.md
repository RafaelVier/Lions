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

- [x] F0.1 `build.gradle.kts`: `com.google.zxing:core:3.5.3` e `com.google.zxing:javase:3.5.3`; `@EnableScheduling` na aplicação.
- [x] F0.2 Enums do `plan.md §2` (criar/alterar/remover valores).
- [x] F0.3 Entidades de domínio do `plan.md §3` com regras + testes unitários das máquinas de estado (Equipment/RN12, Loan, WaitlistEntry, Maintenance, Beneficiary/RN05-RN07, LoanOrder, User/RN02).
- [x] F0.4 Ports, JPA entities, Spring Data repos e adapters do `plan.md §3–§4` (inclui `@Version` e `findByIdForUpdate` em equipamento; remove `Requester*`).
- [x] F0.5 Serviços compartilhados do `plan.md §5` (Audit, Parameter + controller, CurrentUser, FileStorage, LgpdMasker, CpfValidator, PasswordPolicy, WhatsAppMessageHelper genérico) + `Clock` bean.
- [x] F0.6 Eventos e esqueletos/contratos do `plan.md §6` + DTOs dos contratos.
- [x] F0.7 `ConflictException` + `GlobalExceptionHandler` (`presentation.handler`) conforme `plan.md §1`.
- [x] F0.8 Segurança do `plan.md §9` (roles, rotas públicas, JWT 30 min com `jti`, `TokenRevocationService`, entry point 401/403 JSON, admin inicial configurável, CORS por propriedade, seed só em `dev`).
- [x] F0.9 Ajustar código existente para compilar com o novo modelo (serviços antigos reduzidos ao mínimo; donos da Onda 1 reescrevem).
- [x] F0.10 `src/test/resources/application.yml` com `lions.scheduling.enabled=false`; `./gradlew test` verde.

## Onda 1 — Frentes paralelas

### Frente A — Acesso, ajuda e assistente (RF01, RF13, RF14)
Posse: `application/service/auth/**`, `application/service/user/**`, `application/service/help/**`, `application/service/assistant/**`, `presentation/controller/{Auth,User,Help,Assistant}Controller.java`, DTOs `Auth*`, `Login*`, `User*`, `Password*`, `Help*`, `Assistant*`, remoção de `RegisterRequest`, testes correspondentes.
- [x] A.1 Login/refresh/logout/troca de senha (RN57) com auditoria.
- [x] A.2 CRUD de usuários, perfil, inativação, redefinição de senha (RN01–RN03).
- [ ] A.3 Conteúdo de ajuda (RN53) + FAQ pública + seed `dev`.
- [x] A.4 Assistente por regras: sugestões de alocação e busca de FAQ (RN50, RN51).

### Frente B — Beneficiários, solicitações e histórico do assistido (RF02, RF09, pedido público)
Posse: `application/service/beneficiary/**`, `application/service/order/**`, `presentation/controller/{Beneficiary,LoanOrder,Cep}Controller.java`, DTOs `Beneficiary*`, `LoanOrder*`, `Consent*`, `Cep*` (application), remoção de `LoanApprovalRequest` se não usado, testes correspondentes.
- [x] B.1 CRUD + consentimento + inativação/reativação + anonimização (RN03–RN08, RN34, RN59).
- [x] B.2 Histórico consolidado (RN42).
- [x] B.3 Pedido público + aprovação (→ `LoanService.registerLoan`) + rejeição + envio à fila (→ `WaitlistService.enqueue`).
- [x] B.4 Consulta CEP.

### Frente C — Equipamentos, QR Code e manutenção (RF03, RF04, RF08)
Posse: `application/service/equipment/**`, `application/service/maintenance/**`, `infrastructure/qrcode/**`, `infrastructure/scheduler/PreventiveMaintenanceJob.java`, `presentation/controller/{Equipment,Maintenance}Controller.java`, DTOs `Equipment*`, `Maintenance*`, `WriteOff*`, `QrCode*`, `Preventive*`, testes correspondentes.
- [x] C.1 Cadastro com patrimônio gerado, edição restrita, listagem/filtros, baixa (RN09, RN11–RN14).
- [x] C.2 QR Code PNG e leitura com operações disponíveis (RN10, RN15).
- [x] C.3 Manutenção: abertura, custos/ajustes, encerramento com parecer, listener de avaria (RN24, RN35–RN38, RN41).
- [x] C.4 Preventiva: `PreventiveMaintenanceService.findDue` + job (RN39).
- [x] C.5 Histórico do equipamento.

### Frente D — Empréstimos, termo, ocorrências e auditoria (RF05, RF07, RF10)
Posse: `application/service/loan/**` (inclui `LoanService`, `BeneficiaryRestrictionService`, `PdfTermService`, `OccurrenceService`), `infrastructure/scheduler/OverdueLoanJob.java`, `presentation/controller/{Loan,Occurrence,Audit}Controller.java`, DTOs `Loan*` (exceto contratos — só acréscimo), `Signature*`, `Renewal*`, `Return*`, `Occurrence*`, `Audit*`, `Adjustment*`, remoção de `TermAcceptanceRequest`/`LoanReturnRequest` antigos se substituídos, testes correspondentes.
- [x] D.1 Registro de empréstimo (RN16–RN22, RN28) + termo PDF armazenado (RN06, RN21, RN61).
- [x] D.2 Assinatura digital e física (RN21, RN25).
- [x] D.3 Renovação (RN23), devolução com vistoria + ocorrência + eventos (RN24, RN40), cancelamento (RN26), ajustes (RN41).
- [x] D.4 `BeneficiaryRestrictionService` (RN08, RN20).
- [x] D.5 Job de atraso/inadimplência (RN27) + ocorrências (listar/resolver).
- [x] D.6 Consulta de auditoria (RN58) e histórico de empréstimos com filtros (RF10).

### Frente E — Fila de espera e notificações (RF06, RF12)
Posse: `application/service/waitlist/**`, `application/service/notification/**`, `infrastructure/notification/**`, `infrastructure/scheduler/{ReservationExpiration,DueDateReminder,NotificationRetry,DailySummary}Job.java`, `presentation/controller/{Waitlist,Notification}Controller.java`, DTOs `Waitlist*`, `Priority*`, `Notification*`, `DailySummary*`, templates em `WhatsAppMessageHelper` (só acréscimo), testes correspondentes.
- [x] E.1 Entrada na fila, ordenação, prioridade, recusa, remoção (RN29–RN31, RN33, RN34).
- [x] E.2 Reserva automática por evento + expiração (RN32, RN33).
- [x] E.3 Envio por canal autorizado, porta de envio + adaptador de log/wa.me, retentativas e contato manual (RN47, RN48).
- [x] E.4 Régua de lembretes (RN46) e resumo diário (RN49).

### Frente F — Dashboard e relatórios (RF15, RF11)
Posse: `application/service/dashboard/**`, `application/service/report/**`, `presentation/controller/{Dashboard,Report}Controller.java`, DTOs `Dashboard*`, `Report*`, remoção de `DashboardIndicatorsResponse`, testes correspondentes.
- [x] F.1 Indicadores do RF15 (ocupação, itens em atraso, fila atual por tipo, custos de manutenção, volume de empréstimos) com filtros e visão por perfil (RN54–RN56).
- [x] F.2 Relatórios CSV/PDF, mascaramento por perfil, período padrão e auditoria de exportação (RN43–RN45, RN55).

### Frente G — Alinhamento à planilha e ao MER (revisão de 24/11/2026)
Origem: validação da spec contra as abas "Requisitos - Final", "Regras de negócio - Sugestões", "Casos de Uso" e
"Banco de Dados" (MER). Detalhes em `spec.md` (itens **[rev. 24/11]**) e `docs/banco-de-dados/README.md`.

Feito:
- [x] G.1 Só CPF (RN04): remove `DocumentType`; `cpf`/`legalGuardianCpf` validados; `legalGuardianEmail` (MER).
- [x] G.2 Endereço em tabela própria `tb_addresses` (1:1, MER `tb_endereco`).
- [x] G.3 Tipo de equipamento como tabela `tb_equipment_types` com prazo por tipo (RN18) + `GET/PUT /tipos-equipamento`; remove `loan.days.<TIPO>` dos parâmetros.
- [x] G.4 Ano e especificações do equipamento (MER `ano`, `tb_especificacao`).
- [x] G.5 Script PostgreSQL `V1__init_schema.sql` (Flyway no perfil prod, `ddl-auto: validate`); suíte verde em H2 e em PostgreSQL.
- [x] G.6 MER ajustado (`docs/banco-de-dados/MER.json`) + diagrama e mapeamento lógico → físico.

Ajustes de regra (por frente dona):
- [x] G.7 (E) RN34: beneficiário bloqueado pela RN20 — inclusive **termo não assinado** — sai da fila (revoga a decisão 6 do `andamento.md`).
- [x] G.8 (D) RN40: devolução `PRECISA_MANUTENCAO` também gera ocorrência `DANO`.
- [x] G.9 (C) UC23: encerramento de manutenção só pelo Admin (os dois pareceres).
- [x] G.10 (D) UC16: renovação pública pelo beneficiário (`POST /emprestimos/renovacao-publica`, CPF + nascimento + patrimônio, limite de tentativas).
- [x] G.11 (E) RF12: aviso de "elegível para renovação" no lembrete de 7 dias.
- [x] G.12 (E) RN47: adaptadores de envio real — e-mail (SMTP) e SMS/WhatsApp (provedor configurável); contato manual só como contingência (revoga o padrão `manual` da decisão 4).
- [x] G.13 (A) RN57: sessões persistidas (`tb_user_sessions`, migration V2), `SessionExpirationJob` auditando `SESSION_EXPIRED`, revogação persistida.
- [x] G.14 (D) RN60/RNF06: `POST /sincronizacao` + conflitos (`tb_sync_operations`, `tb_sync_conflicts`, migration V2).

## Onda 2 — Integração
- [x] I.1 Merge das frentes; resolver conflitos em ports/adapters. (Frentes A–G já mergeadas na `wip/onda1-servicos`
  pelos PRs #1–#3, sem conflitos pendentes; removidos 3 imports sem uso. Sem classes órfãs: as não referenciadas são
  beans do Spring ou entidades fora da spec mantidas de propósito.)
- [x] I.2 Teste de integração ponta a ponta: pedido → aprovação → assinatura → devolução com avaria → manutenção → APTO → reserva para a fila.
  (`presentation/controller/FluxoPontaAPontaIT`, só pela API com JWT real e sem `@Transactional`; cobre também termo
  PDF, ocorrência DANO, UC23, renovação pública UC16, sincronização com conflito RN60, dashboard e relatório CSV.)
- [x] I.3 `./gradlew test` verde. (239 testes em H2 e em PostgreSQL 16 com Flyway + `ddl-auto: validate`.)
- [x] I.4 Atualizar `README.md`, `backend/README.md` e `docs/DESCRICAO_DO_PROJETO_E_BACKEND.md`.

## Definição de pronto (por frente)
- Compila, `./gradlew test` verde no clone.
- Cada RN da frente com teste automatizado.
- Endpoints documentados no Swagger (`@Tag`, `@Operation`).
- Sem acesso direto a `LocalDate.now()`/`LocalDateTime.now()` fora do `Clock`.
- Resumo final: arquivos criados/alterados, exceções de posse e pendências.
