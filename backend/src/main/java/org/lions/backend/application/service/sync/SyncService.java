package org.lions.backend.application.service.sync;

import lombok.extern.slf4j.Slf4j;
import org.lions.backend.application.dto.request.LoanCreateRequest;
import org.lions.backend.application.dto.request.LoanReturnRequest;
import org.lions.backend.application.dto.request.SyncBatchRequest;
import org.lions.backend.application.dto.request.SyncOperationRequest;
import org.lions.backend.application.dto.response.LoanResponse;
import org.lions.backend.application.dto.response.SyncConflictResponse;
import org.lions.backend.application.dto.response.SyncOperationResult;
import org.lions.backend.application.port.CurrentUserPort;
import org.lions.backend.application.service.audit.AuditService;
import org.lions.backend.application.service.loan.LoanService;
import org.lions.backend.application.service.notification.NotificationService;
import org.lions.backend.application.support.AuditDetails;
import org.lions.backend.domain.entity.Loan;
import org.lions.backend.domain.entity.SyncConflict;
import org.lions.backend.domain.entity.SyncOperation;
import org.lions.backend.domain.enums.AuditAction;
import org.lions.backend.domain.enums.LoanStatus;
import org.lions.backend.domain.enums.NotificationType;
import org.lions.backend.domain.enums.SyncConflictStatus;
import org.lions.backend.domain.enums.SyncOperationType;
import org.lions.backend.domain.enums.SyncPrevailing;
import org.lions.backend.domain.enums.SyncResultStatus;
import org.lions.backend.domain.exception.BusinessException;
import org.lions.backend.domain.exception.ConflictException;
import org.lions.backend.domain.exception.ForbiddenOperationException;
import org.lions.backend.domain.exception.ResourceNotFoundException;
import org.lions.backend.domain.repository.LoanRepositoryPort;
import org.lions.backend.domain.repository.SyncConflictRepositoryPort;
import org.lions.backend.domain.repository.SyncOperationRepositoryPort;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Sincronização das operações feitas sem conexão (RNF06/RN60). O lote é aplicado em ordem de data/hora do
 * dispositivo, cada operação na sua própria transação (uma recusa não desfaz as demais), com as mesmas regras da
 * operação online. Operação repetida ({@code clientOperationId} já recebido) não é reaplicada.
 * <p>
 * Conflito = a operação recebida é incompatível com um registro já existente sobre o mesmo equipamento (retirada de
 * item que já está emprestado; devolução de empréstimo já devolvido). A operação recebida não é aplicada e o conflito
 * fica pendente para conferência manual, indicando qual registro deve prevalecer pela data/hora mais antiga; o Admin
 * é notificado.
 */
@Slf4j
@Service
public class SyncService {

    public static final String RULE = "RN60";
    public static final String CONFLICT_ENTITY = "SyncConflict";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SyncOperationRepositoryPort operationRepository;
    private final SyncConflictRepositoryPort conflictRepository;
    private final LoanRepositoryPort loanRepository;
    private final LoanService loanService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final CurrentUserPort currentUser;
    private final Clock clock;
    private final TransactionTemplate tx;

    public SyncService(SyncOperationRepositoryPort operationRepository, SyncConflictRepositoryPort conflictRepository,
                       LoanRepositoryPort loanRepository, LoanService loanService,
                       NotificationService notificationService, AuditService auditService,
                       CurrentUserPort currentUser, Clock clock, PlatformTransactionManager transactionManager) {
        this.operationRepository = operationRepository;
        this.conflictRepository = conflictRepository;
        this.loanRepository = loanRepository;
        this.loanService = loanService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.clock = clock;
        this.tx = new TransactionTemplate(transactionManager);
    }

    private record Outcome(SyncResultStatus status, String message, Long loanId, SyncConflict conflict) {

        static Outcome applied(String message, Long loanId) {
            return new Outcome(SyncResultStatus.APLICADA, message, loanId, null);
        }

        static Outcome rejected(String message) {
            return new Outcome(SyncResultStatus.RECUSADA, message, null, null);
        }
    }

    /** Aplica o lote (não transacional: cada operação tem a sua transação). Resultados em ordem de aplicação. */
    public List<SyncOperationResult> process(SyncBatchRequest batch) {
        String username = currentUser.username();
        LocalDateTime receivedAt = LocalDateTime.now(clock);
        List<SyncOperationRequest> ordered = new ArrayList<>(batch.operations());
        ordered.sort(Comparator.comparing(SyncOperationRequest::occurredAt));

        List<SyncOperationResult> results = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (SyncOperationRequest op : ordered) {
            String opId = op.clientOperationId().trim();
            if (!seen.add(opId)) {
                results.add(new SyncOperationResult(opId, op.type(), SyncResultStatus.DUPLICADA,
                        "Operação repetida no mesmo lote.", null, null));
                continue;
            }
            Optional<SyncOperation> previous = tx.execute(s -> operationRepository.findByClientOperationId(opId));
            if (previous != null && previous.isPresent()) {
                results.add(duplicate(previous.get()));
                continue;
            }
            Outcome outcome;
            try {
                outcome = tx.execute(s -> apply(op));
            } catch (BusinessException | ConflictException | ForbiddenOperationException
                     | ResourceNotFoundException e) {
                outcome = Outcome.rejected(e.getMessage());
            } catch (DataAccessException e) {
                log.warn("Sincronização: operação {} recusada por erro de dados: {}", opId, e.getMessage());
                outcome = Outcome.rejected("Não foi possível aplicar a operação (dados alterados por outra "
                        + "operação). Confira e registre novamente se necessário.");
            }
            Outcome finalOutcome = outcome;
            try {
                results.add(tx.execute(s -> record(op, opId, finalOutcome, username, receivedAt)));
            } catch (DataIntegrityViolationException e) {
                // mesma operação enviada em paralelo por outra requisição
                results.add(new SyncOperationResult(opId, op.type(), SyncResultStatus.DUPLICADA,
                        "Operação já recebida em outra sincronização.", null, null));
            }
        }
        return results;
    }

    @Transactional(readOnly = true)
    public List<SyncConflictResponse> findConflicts(SyncConflictStatus status) {
        return conflictRepository.findAll(status).stream().map(SyncConflictResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SyncConflictResponse findConflict(Long id) {
        return SyncConflictResponse.from(findConflictEntity(id));
    }

    /** Conferência manual do conflito (somente Admin), com nota de como foi resolvido. */
    @Transactional
    public SyncConflictResponse resolve(Long id, String note) {
        SyncConflict conflict = findConflictEntity(id);
        conflict.resolve(note, currentUser.username(), LocalDateTime.now(clock));
        SyncConflict saved = conflictRepository.save(conflict);
        auditService.record(CONFLICT_ENTITY, id, AuditAction.STATUS_CHANGE, AuditDetails.of(
                "status", AuditDetails.oldNew(SyncConflictStatus.PENDENTE, SyncConflictStatus.RESOLVIDO),
                "resolutionNote", saved.getResolutionNote()));
        return SyncConflictResponse.from(saved);
    }

    // ---------- aplicação ----------

    private Outcome apply(SyncOperationRequest op) {
        return switch (op.type()) {
            case RETIRADA -> applyCheckout(op);
            case DEVOLUCAO -> applyReturn(op);
            case RENOVACAO -> applyRenewal(op);
        };
    }

    private Outcome applyCheckout(SyncOperationRequest op) {
        require(op.beneficiaryId(), "beneficiaryId");
        require(op.equipmentId(), "equipmentId");
        Optional<Loan> open = loanRepository.findByEquipmentId(op.equipmentId()).stream()
                .filter(Loan::isOpen).findFirst();
        if (open.isPresent()) {
            Loan existing = open.get();
            return conflict(op, op.equipmentId(), existing, operationTime(existing, SyncOperationType.RETIRADA,
                            existing.getCreatedAt() != null ? existing.getCreatedAt()
                                    : existing.getLoanDate().atStartOfDay()),
                    "Retirada offline do equipamento, que já está no empréstimo #" + existing.getId() + ".");
        }
        LoanResponse loan = loanService.registerOfflineLoan(LoanCreateRequest.builder()
                .beneficiaryId(op.beneficiaryId())
                .equipmentId(op.equipmentId())
                .deliveryNotes(op.deliveryNotes())
                .build(), op.occurredAt());
        return Outcome.applied("Retirada registrada (termo pendente de assinatura).", loan.getId());
    }

    private Outcome applyReturn(SyncOperationRequest op) {
        require(op.loanId(), "loanId");
        if (op.returnCondition() == null) {
            throw new BusinessException("Informe returnCondition para a devolução.");
        }
        Loan loan = loanRepository.findById(op.loanId())
                .orElseThrow(() -> new ResourceNotFoundException("Empréstimo", op.loanId()));
        if (loan.getStatus() == LoanStatus.DEVOLVIDO) {
            return conflict(op, loan.getEquipment() != null ? loan.getEquipment().getId() : null, loan,
                    operationTime(loan, SyncOperationType.DEVOLUCAO, loan.getReturnedAt()),
                    "Devolução offline de empréstimo que já foi devolvido.");
        }
        loanService.registerReturn(op.loanId(), new LoanReturnRequest(op.returnCondition(), op.damageDescription(),
                null, op.occurredAt().toLocalDate()));
        return Outcome.applied("Devolução registrada.", op.loanId());
    }

    private Outcome applyRenewal(SyncOperationRequest op) {
        require(op.loanId(), "loanId");
        loanService.renew(op.loanId());
        return Outcome.applied("Renovação registrada.", op.loanId());
    }

    private Outcome conflict(SyncOperationRequest op, Long equipmentId, Loan existing, LocalDateTime existingAt,
                             String description) {
        SyncPrevailing prevailing = SyncConflict.prevailing(existingAt, op.occurredAt());
        SyncConflict conflict = SyncConflict.builder()
                .equipmentId(equipmentId)
                .existingLoanId(existing.getId())
                .operationType(op.type())
                .incomingOccurredAt(op.occurredAt())
                .existingOccurredAt(existingAt)
                .prevailing(prevailing)
                .description(description)
                .status(SyncConflictStatus.PENDENTE)
                .build();
        String message = description + " Prevalece: " + prevailing.getDescription()
                + (prevailing == SyncPrevailing.RECEBIDA
                ? " — corrija manualmente o registro existente."
                : " — a operação offline não foi aplicada.");
        return new Outcome(SyncResultStatus.CONFLITO, message, existing.getId(), conflict);
    }

    /**
     * Quando a operação existente aconteceu: a data/hora do dispositivo, se ela veio de sincronização; senão a do
     * servidor ({@code fallback}).
     */
    private LocalDateTime operationTime(Loan loan, SyncOperationType type, LocalDateTime fallback) {
        return operationRepository.findApplied(loan.getId(), type).map(SyncOperation::getOccurredAt).orElse(fallback);
    }

    private SyncOperationResult record(SyncOperationRequest op, String opId, Outcome outcome, String username,
                                       LocalDateTime receivedAt) {
        Long conflictId = null;
        if (outcome.conflict() != null) {
            SyncConflict conflict = outcome.conflict();
            conflict.setClientOperationId(opId);
            conflict.setUsername(username);
            SyncConflict saved = conflictRepository.save(conflict);
            conflictId = saved.getId();
            auditService.recordWithRule(CONFLICT_ENTITY, saved.getId(), AuditAction.CREATE, RULE, AuditDetails.of(
                    "clientOperationId", opId, "operationType", op.type(), "equipmentId", saved.getEquipmentId(),
                    "existingLoanId", saved.getExistingLoanId(), "prevailing", saved.getPrevailing()));
            notificationService.notifyAdmins(NotificationType.CONFLITO_SINCRONIZACAO,
                    "Conflito de sincronização #" + saved.getId() + " (" + op.type().getDescription() + " feita por "
                            + username + " em " + DATE_TIME.format(op.occurredAt()) + "): " + outcome.message(),
                    "SYNC:" + opId, receivedAt.toLocalDate());
        }
        operationRepository.save(SyncOperation.builder()
                .clientOperationId(opId)
                .type(op.type())
                .beneficiaryId(op.beneficiaryId())
                .equipmentId(op.equipmentId())
                .loanId(op.loanId())
                .occurredAt(op.occurredAt())
                .receivedAt(receivedAt)
                .username(username)
                .result(outcome.status())
                .message(truncate(outcome.message()))
                .resultingLoanId(outcome.status() == SyncResultStatus.APLICADA ? outcome.loanId() : null)
                .conflictId(conflictId)
                .build());
        return new SyncOperationResult(opId, op.type(), outcome.status(), outcome.message(),
                outcome.loanId(), conflictId);
    }

    private static SyncOperationResult duplicate(SyncOperation previous) {
        return new SyncOperationResult(previous.getClientOperationId(), previous.getType(), SyncResultStatus.DUPLICADA,
                "Operação já processada em " + DATE_TIME.format(previous.getReceivedAt()) + " ("
                        + previous.getResult().getDescription() + "): " + previous.getMessage(),
                previous.getResultingLoanId(), previous.getConflictId());
    }

    private SyncConflict findConflictEntity(Long id) {
        return conflictRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conflito de sincronização", id));
    }

    private static void require(Long value, String field) {
        if (value == null) {
            throw new BusinessException("Campo obrigatório para esta operação: " + field + ".");
        }
    }

    private static String truncate(String message) {
        return message == null || message.length() <= 1000 ? message : message.substring(0, 997) + "...";
    }
}
