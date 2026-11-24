package org.lions.backend.application.service.loan;

import org.lions.backend.application.support.CpfValidator;

import org.lions.backend.application.dto.response.PublicRenewalResponse;

import org.lions.backend.application.dto.request.PublicRenewalRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lions.backend.application.dto.request.AdjustmentRequest;
import org.lions.backend.application.dto.request.DigitalSignatureRequest;
import org.lions.backend.application.dto.request.LoanCreateRequest;
import org.lions.backend.application.dto.request.LoanReturnRequest;
import org.lions.backend.application.dto.response.AdjustmentResponse;
import org.lions.backend.application.dto.response.BeneficiaryRestriction;
import org.lions.backend.application.dto.response.LoanRenewalResponse;
import org.lions.backend.application.dto.response.LoanResponse;
import org.lions.backend.application.dto.response.LoanReturnResponse;
import org.lions.backend.application.event.BeneficiaryRestrictedEvent;
import org.lions.backend.application.event.EquipmentAvailableEvent;
import org.lions.backend.application.event.EquipmentDamagedOnReturnEvent;
import org.lions.backend.application.port.CurrentUserPort;
import org.lions.backend.application.port.FileStoragePort;
import org.lions.backend.application.service.audit.AuditService;
import org.lions.backend.application.service.parameter.ParameterService;
import org.lions.backend.application.service.waitlist.WaitlistService;
import org.lions.backend.application.support.AuditDetails;
import org.lions.backend.application.support.WhatsAppMessageHelper;
import org.lions.backend.domain.entity.AuditLog;
import org.lions.backend.domain.entity.Beneficiary;
import org.lions.backend.domain.entity.Equipment;
import org.lions.backend.domain.entity.Loan;
import org.lions.backend.domain.entity.LoanOrder;
import org.lions.backend.domain.entity.LoanRenewal;
import org.lions.backend.domain.entity.LoanReturn;
import org.lions.backend.domain.entity.Occurrence;
import org.lions.backend.domain.entity.RenewalPolicy;
import org.lions.backend.domain.enums.AuditAction;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.LoanStatus;
import org.lions.backend.domain.enums.OccurrenceStatus;
import org.lions.backend.domain.enums.OccurrenceType;
import org.lions.backend.domain.enums.OrderStatus;
import org.lions.backend.domain.enums.SignatureType;
import org.lions.backend.domain.enums.UserRole;
import org.lions.backend.domain.exception.BusinessException;
import org.lions.backend.domain.exception.ConflictException;
import org.lions.backend.domain.exception.ForbiddenOperationException;
import org.lions.backend.domain.exception.ResourceNotFoundException;
import org.lions.backend.domain.repository.BeneficiaryRepositoryPort;
import org.lions.backend.domain.repository.EquipmentRepositoryPort;
import org.lions.backend.domain.repository.LoanOrderRepositoryPort;
import org.lions.backend.domain.repository.LoanRenewalRepositoryPort;
import org.lions.backend.domain.repository.LoanRepositoryPort;
import org.lions.backend.domain.repository.LoanReturnRepositoryPort;
import org.lions.backend.domain.repository.OccurrenceRepositoryPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Empréstimos (RF05/RF07): registro com termo em PDF (RN16–RN22, RN28, RN61), assinatura (RN21), renovação (RN23),
 * devolução com vistoria (RN24, RN40), cancelamento (RN26) e ajustes (RN41). Contrato §6.2: {@link #registerLoan}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {

    public static final String ENTITY_NAME = "Loan";
    public static final String EQUIPMENT_ENTITY = "Equipment";
    public static final String TERMS_FOLDER = "termos";
    /** Autor das renovações feitas pelo próprio beneficiário na rota pública (UC16). */
    public static final String BENEFICIARY_AUTHOR = "BENEFICIARIO";
    public static final String SIGNED_TERMS_FOLDER = "termos/assinados";
    public static final long MAX_SIGNED_FILE_BYTES = 10L * 1024 * 1024;
    private static final Map<String, String> SIGNED_FILE_TYPES = Map.of(
            "application/pdf", "pdf", "image/jpeg", "jpg", "image/png", "png");
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final LoanRepositoryPort loanRepository;
    private final LoanRenewalRepositoryPort renewalRepository;
    private final LoanReturnRepositoryPort returnRepository;
    private final OccurrenceRepositoryPort occurrenceRepository;
    private final BeneficiaryRepositoryPort beneficiaryRepository;
    private final EquipmentRepositoryPort equipmentRepository;
    private final LoanOrderRepositoryPort orderRepository;
    private final BeneficiaryRestrictionService restrictionService;
    private final WaitlistService waitlistService;
    private final ParameterService parameterService;
    private final PdfTermService pdfTermService;
    private final FileStoragePort fileStorage;
    private final AuditService auditService;
    private final CurrentUserPort currentUser;
    private final WhatsAppMessageHelper whatsApp;
    private final ApplicationEventPublisher events;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public record StoredFile(String fileName, String contentType, byte[] content) {
    }

    public record LoanFilter(LoanStatus status, Long beneficiaryId, Long equipmentId, LocalDate start,
                             LocalDate end, EquipmentType equipmentType) {
    }

    // ---------- contrato §6.2 ----------

    /**
     * Registra empréstimo (RN17–RN22, RN28): valida beneficiário/equipamento, gera e armazena o termo e marca o item
     * como EMPRESTADO. O empréstimo nasce PENDENTE_ASSINATURA (RN21).
     */
    @Transactional
    public LoanResponse registerLoan(LoanCreateRequest req) {
        return registerLoan(req, false);
    }

    /**
     * RN60 — retirada registrada sem conexão e sincronizada depois: a data de retirada é a do dispositivo (pode ser
     * anterior a hoje sem exigir Admin; a justificativa de retroativo é preenchida automaticamente).
     */
    @Transactional
    public LoanResponse registerOfflineLoan(LoanCreateRequest req, LocalDateTime occurredAt) {
        req.setLoanDate(occurredAt.toLocalDate());
        if (occurredAt.toLocalDate().isBefore(LocalDate.now(clock))
                && (req.getRetroactiveJustification() == null || req.getRetroactiveJustification().isBlank())) {
            req.setRetroactiveJustification("Retirada registrada offline em " + occurredAt
                    + " e sincronizada (RN60).");
        }
        return registerLoan(req, true);
    }

    private LoanResponse registerLoan(LoanCreateRequest req, boolean offline) {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);
        String username = currentUser.username();

        Beneficiary beneficiary = beneficiaryRepository.findById(req.getBeneficiaryId())
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiário", req.getBeneficiaryId()));
        BeneficiaryRestriction restriction = restrictionService.evaluate(beneficiary.getId());
        if (restriction.blocked()) {
            throw new BusinessException("Beneficiário bloqueado para novos empréstimos: "
                    + String.join(" ", restriction.reasons()));
        }

        LocalDate loanDate = req.getLoanDate() != null ? req.getLoanDate() : today;
        if (loanDate.isAfter(today)) {
            throw new BusinessException("A data de retirada não pode ser futura.");
        }
        if (loanDate.isBefore(today) && !offline && !currentUser.hasRole(UserRole.ROLE_ADMIN)) {
            throw new ForbiddenOperationException("Empréstimo retroativo só pode ser registrado por Administrador.");
        }

        Equipment equipment = equipmentRepository.findByIdForUpdate(req.getEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento", req.getEquipmentId()));
        ensureLendable(equipment, beneficiary.getId());

        List<Loan> openLoans = loanRepository.findOpenByBeneficiaryId(beneficiary.getId());
        int maxActive = parameterService.maxActiveLoansPerBeneficiary();
        if (openLoans.size() >= maxActive) {
            throw new BusinessException("Beneficiário já possui " + openLoans.size()
                    + " empréstimo(s) em aberto (máximo " + maxActive + ").");
        }
        if (openLoans.stream().anyMatch(l -> l.getEquipment() != null
                && l.getEquipment().getType() == equipment.getType())) {
            throw new BusinessException("Beneficiário já possui empréstimo em aberto de "
                    + equipment.getType().getDescription() + " (máximo 1 por tipo).");
        }

        LoanOrder order = null;
        if (req.getOrderId() != null) {
            order = orderRepository.findById(req.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Solicitação", req.getOrderId()));
            if (!Objects.equals(order.getBeneficiary().getId(), beneficiary.getId())) {
                throw new BusinessException("A solicitação informada não pertence ao beneficiário.");
            }
            OrderStatus previous = order.getStatus();
            order.approve(username, now);
            order = orderRepository.save(order);
            auditService.record("LoanOrder", order.getId(), AuditAction.STATUS_CHANGE,
                    AuditDetails.of("status", AuditDetails.oldNew(previous, order.getStatus())));
        }

        Loan loan = Loan.builder()
                .beneficiary(beneficiary)
                .equipment(equipment)
                .order(order)
                .status(LoanStatus.PENDENTE_ASSINATURA)
                .loanDate(loanDate)
                .expectedReturnDate(loanDate.plusDays(parameterService.loanDays(equipment.getType())))
                .retroactiveJustification(blankToNull(req.getRetroactiveJustification()))
                .initialCondition(equipment.getCondition())
                .deliveryNotes(blankToNull(req.getDeliveryNotes()))
                .deliveryPhotoUrl(blankToNull(req.getDeliveryPhotoUrl()))
                .createdBy(username)
                .build();
        loan.validateNew(today);

        EquipmentStatus previousEquipmentStatus = equipment.getStatus();
        equipment.markAsLoaned(beneficiary.getId());
        Equipment loanedEquipment = equipmentRepository.save(equipment);
        loan.setEquipment(loanedEquipment);
        Loan saved = loanRepository.save(loan);

        // RN21/RN61 — termo gerado, armazenado e vinculado (sem endpoint de exclusão)
        PdfTermService.GeneratedTerm term = pdfTermService.generateResponsibilityTerm(saved);
        String termPath = fileStorage.store(TERMS_FOLDER,
                "termo-emprestimo-" + saved.getId() + "-" + FILE_TIMESTAMP.format(now) + ".pdf", term.pdfBytes());
        saved.setTermPdfPath(termPath);
        saved.setTermHashSha256(term.sha256Hash());
        saved.setTermGeneratedAt(now);
        saved = loanRepository.save(saved);

        waitlistService.markServed(beneficiary.getId(), loanedEquipment.getId(), loanedEquipment.getType());
        // RN34/RN20 — com o termo pendente de assinatura o beneficiário fica bloqueado: sai das demais filas
        events.publishEvent(new BeneficiaryRestrictedEvent(beneficiary.getId(),
                "Termo de empréstimo pendente de assinatura."));

        auditService.record(ENTITY_NAME, saved.getId(), AuditAction.CREATE, AuditDetails.of(
                "beneficiaryId", beneficiary.getId(),
                "equipmentId", loanedEquipment.getId(),
                "patrimonyNumber", loanedEquipment.getPatrimonyNumber(),
                "loanDate", saved.getLoanDate(),
                "expectedReturnDate", saved.getExpectedReturnDate(),
                "retroactive", saved.isRetroactive(),
                "retroactiveJustification", saved.getRetroactiveJustification(),
                "orderId", order != null ? order.getId() : null,
                "termHashSha256", saved.getTermHashSha256()));
        auditService.record(EQUIPMENT_ENTITY, loanedEquipment.getId(), AuditAction.STATUS_CHANGE, AuditDetails.of(
                "status", AuditDetails.oldNew(previousEquipmentStatus, loanedEquipment.getStatus()),
                "loanId", saved.getId()));
        return detail(saved);
    }

    // ---------- consultas ----------

    @Transactional(readOnly = true)
    public List<LoanResponse> findAll(LoanFilter filter) {
        List<Loan> loans;
        if (filter.beneficiaryId() != null) {
            loans = loanRepository.findByBeneficiaryId(filter.beneficiaryId());
        } else if (filter.equipmentId() != null) {
            loans = loanRepository.findByEquipmentId(filter.equipmentId());
        } else if (filter.start() != null || filter.end() != null) {
            loans = loanRepository.findByLoanDateBetween(
                    filter.start() != null ? filter.start() : LocalDate.of(1900, 1, 1),
                    filter.end() != null ? filter.end() : LocalDate.of(9999, 12, 31));
        } else if (filter.status() != null) {
            loans = loanRepository.findByStatus(filter.status());
        } else {
            loans = loanRepository.findAll();
        }
        return loans.stream()
                .filter(l -> filter.status() == null || l.getStatus() == filter.status())
                .filter(l -> filter.equipmentId() == null
                        || (l.getEquipment() != null && filter.equipmentId().equals(l.getEquipment().getId())))
                .filter(l -> filter.start() == null || !l.getLoanDate().isBefore(filter.start()))
                .filter(l -> filter.end() == null || !l.getLoanDate().isAfter(filter.end()))
                .filter(l -> filter.equipmentType() == null
                        || (l.getEquipment() != null && l.getEquipment().getType() == filter.equipmentType()))
                .sorted(Comparator.comparing(Loan::getLoanDate).reversed()
                        .thenComparing(Loan::getId, Comparator.reverseOrder()))
                .map(this::toResponse)
                .toList();
    }

    /** Detalhe com renovações, devolução, ajustes e elegibilidade de renovação. */
    @Transactional(readOnly = true)
    public LoanResponse findById(Long id) {
        return detail(find(id));
    }

    /** RN61 — termo PDF armazenado. */
    @Transactional(readOnly = true)
    public StoredFile getTerm(Long id) {
        Loan loan = find(id);
        if (loan.getTermPdfPath() == null) {
            throw new ResourceNotFoundException("Termo do empréstimo " + id + " não encontrado.");
        }
        return new StoredFile("termo-emprestimo-" + id + ".pdf", "application/pdf",
                fileStorage.read(loan.getTermPdfPath()));
    }

    /** Termo assinado digitalizado (assinatura física — RN21). */
    @Transactional(readOnly = true)
    public StoredFile getSignedTerm(Long id) {
        Loan loan = find(id);
        if (loan.getSignedDocumentPath() == null) {
            throw new ResourceNotFoundException("Termo assinado do empréstimo " + id + " não encontrado.");
        }
        String path = loan.getSignedDocumentPath();
        String extension = path.substring(path.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        String contentType = SIGNED_FILE_TYPES.entrySet().stream()
                .filter(entry -> entry.getValue().equals(extension))
                .map(Map.Entry::getKey)
                .findFirst().orElse("application/octet-stream");
        return new StoredFile("termo-assinado-emprestimo-" + id + "." + extension, contentType,
                fileStorage.read(path));
    }

    // ---------- assinatura (RN21) ----------

    @Transactional
    public LoanResponse registerDigitalSignature(Long id, DigitalSignatureRequest request) {
        Loan loan = find(id);
        ensurePendingSignature(loan);
        String signer = resolveSigner(loan, request != null ? request.signerName() : null);
        loan.registerSignature(SignatureType.DIGITAL, signer, null, currentUser.username(), LocalDateTime.now(clock));
        Loan saved = loanRepository.save(loan);
        auditSignature(saved);
        return detail(saved);
    }

    @Transactional
    public LoanResponse registerPhysicalSignature(Long id, String signerName, byte[] content, String originalFileName,
                                                  String contentType) {
        Loan loan = find(id);
        ensurePendingSignature(loan);
        String signer = resolveSigner(loan, signerName);
        if (content == null || content.length == 0) {
            throw new BusinessException("Envie o arquivo do termo assinado (PDF, JPG ou PNG).");
        }
        if (content.length > MAX_SIGNED_FILE_BYTES) {
            throw new BusinessException("O arquivo do termo assinado deve ter no máximo 10 MB.");
        }
        String extension = signedFileExtension(contentType, originalFileName);
        LocalDateTime now = LocalDateTime.now(clock);
        String path = fileStorage.store(SIGNED_TERMS_FOLDER,
                "termo-assinado-" + id + "-" + FILE_TIMESTAMP.format(now) + "." + extension, content);
        loan.registerSignature(SignatureType.FISICA_DIGITALIZADA, signer, path, currentUser.username(), now);
        Loan saved = loanRepository.save(loan);
        auditSignature(saved);
        return detail(saved);
    }

    // ---------- renovação (RN23) ----------

    @Transactional
    public LoanResponse renew(Long id) {
        return detail(applyRenewal(find(id), currentUser.username()));
    }

    /**
     * UC16 — renovação pelo próprio beneficiário, sem login. Confere CPF + data de nascimento + patrimônio contra um
     * empréstimo em aberto; qualquer divergência devolve a mesma mensagem genérica (não revela se o CPF existe).
     * Autor registrado como {@value #BENEFICIARY_AUTHOR} (RN25).
     */
    @Transactional
    public PublicRenewalResponse renewByBeneficiary(PublicRenewalRequest request) {
        String cpf = CpfValidator.normalize(request.cpf());
        String patrimony = request.patrimonyNumber().trim().toUpperCase(java.util.Locale.ROOT);
        Loan loan = beneficiaryRepository.findActiveByCpf(cpf)
                .filter(b -> request.birthDate().equals(b.getBirthDate()))
                .flatMap(b -> equipmentRepository.findByPatrimonyNumber(patrimony)
                        .flatMap(e -> loanRepository.findByEquipmentId(e.getId()).stream()
                                .filter(Loan::isOpen)
                                .filter(l -> Objects.equals(l.getBeneficiary().getId(), b.getId()))
                                .findFirst()))
                .orElseThrow(() -> new BusinessException(
                        "Os dados informados não conferem com nenhum empréstimo em aberto."));
        LocalDate previousDue = loan.getExpectedReturnDate();
        Loan saved = applyRenewal(loan, BENEFICIARY_AUTHOR);
        return new PublicRenewalResponse(saved.getEquipment().getPatrimonyNumber(),
                saved.getEquipment().getType().getDescription(), previousDue, saved.getExpectedReturnDate(),
                saved.getRenewalCount(), parameterService.maxRenewals());
    }

    /** RN23 — aplica a renovação (regras no domínio), grava o registro imutável e audita com o autor informado. */
    private Loan applyRenewal(Loan loan, String author) {
        LocalDate today = LocalDate.now(clock);
        EquipmentType type = loan.getEquipment().getType();
        LocalDate previousDue = loan.getExpectedReturnDate();
        int previousCount = loan.getRenewalCount();
        LocalDate newDue = previousDue.plusDays(parameterService.loanDays(type));
        LoanRenewal renewal = loan.renew(newDue, today, renewalPolicy(), waitlistService.hasWaitingFor(type),
                author, LocalDateTime.now(clock));
        Loan saved = loanRepository.save(loan);
        renewalRepository.save(renewal);
        auditService.recordAs(author, ENTITY_NAME, saved.getId(), AuditAction.UPDATE, AuditDetails.of(
                "expectedReturnDate", AuditDetails.oldNew(previousDue, saved.getExpectedReturnDate()),
                "renewalCount", AuditDetails.oldNew(previousCount, saved.getRenewalCount())));
        return saved;
    }

    // ---------- devolução (RN24, RN40) ----------

    @Transactional
    public LoanResponse registerReturn(Long id, LoanReturnRequest request) {
        Loan loan = find(id);
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);
        String username = currentUser.username();

        LocalDate returnDate = request.returnDate() != null ? request.returnDate() : today;
        if (returnDate.isAfter(today)) {
            throw new BusinessException("A data da devolução não pode ser futura.");
        }
        if (returnDate.isBefore(loan.getLoanDate())) {
            throw new BusinessException("A data da devolução não pode ser anterior à data de retirada.");
        }
        if (returnRepository.findByLoanId(id).isPresent()) {
            throw new ConflictException("A devolução deste empréstimo já foi registrada.", Map.of("loanId", id));
        }

        LoanReturn loanReturn = LoanReturn.register(id, returnDate, request.returnCondition(),
                blankToNull(request.damageDescription()), blankToNull(request.returnPhotoUrl()), username);
        LoanStatus previousStatus = loan.getStatus();
        loan.markReturned(now);
        Loan saved = loanRepository.save(loan);
        returnRepository.save(loanReturn);

        Equipment equipment = equipmentRepository.findByIdForUpdate(loan.getEquipment().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento", loan.getEquipment().getId()));
        boolean needsMaintenance = loanReturn.needsMaintenance();
        EquipmentStatus previousEquipmentStatus = equipment.getStatus();
        equipment.markAsReturned(needsMaintenance);
        Equipment returnedEquipment = equipmentRepository.save(equipment);

        auditService.record(ENTITY_NAME, id, AuditAction.STATUS_CHANGE, AuditDetails.of(
                "status", AuditDetails.oldNew(previousStatus, saved.getStatus()),
                "returnDate", returnDate,
                "returnCondition", request.returnCondition(),
                "damageDescription", loanReturn.getDamageDescription()));
        auditService.recordWithRule(EQUIPMENT_ENTITY, returnedEquipment.getId(), AuditAction.STATUS_CHANGE, "RN24",
                AuditDetails.of("status", AuditDetails.oldNew(previousEquipmentStatus, returnedEquipment.getStatus()),
                        "loanId", id));

        Long beneficiaryId = loan.getBeneficiary().getId();
        if (loanReturn.isHasDamage()) {
            // RN40 — toda avaria (PRECISA_MANUTENCAO ou DANIFICADO) gera ocorrência vinculada; bloqueia pela RN20
            String damage = loanReturn.getDamageDescription() != null
                    ? loanReturn.getDamageDescription()
                    : "condição " + request.returnCondition().getDescription() + ", sem descrição";
            Occurrence occurrence = occurrenceRepository.save(Occurrence.builder()
                    .beneficiary(loan.getBeneficiary())
                    .loanId(id)
                    .type(OccurrenceType.DANO)
                    .status(OccurrenceStatus.ABERTA)
                    .description("Avaria na devolução do equipamento " + returnedEquipment.getPatrimonyNumber()
                            + ": " + damage)
                    .automatic(true)
                    .createdBy(username)
                    .build());
            auditService.recordAutomatic("Occurrence", occurrence.getId(), AuditAction.CREATE, "RN40",
                    AuditDetails.of("type", OccurrenceType.DANO, "loanId", id, "beneficiaryId", beneficiaryId,
                            "returnCondition", request.returnCondition()));
            events.publishEvent(new BeneficiaryRestrictedEvent(beneficiaryId,
                    "Ocorrência de dano registrada na devolução."));
        }
        if (needsMaintenance) {
            String description = loanReturn.getDamageDescription() != null
                    ? loanReturn.getDamageDescription()
                    : "Devolução com condição: " + request.returnCondition().getDescription() + ".";
            events.publishEvent(new EquipmentDamagedOnReturnEvent(returnedEquipment.getId(), id, beneficiaryId,
                    description));
        } else {
            events.publishEvent(new EquipmentAvailableEvent(returnedEquipment.getId(), returnedEquipment.getType()));
        }
        return detail(find(id));
    }

    // ---------- cancelamento (RN26) e ajustes (RN41) ----------

    /** RN26 — somente Admin (verificado no controller), com motivo; o item volta a DISPONIVEL. */
    @Transactional
    public LoanResponse cancel(Long id, String reason) {
        Loan loan = find(id);
        LoanStatus previous = loan.getStatus();
        loan.cancel(reason, currentUser.username(), LocalDateTime.now(clock));
        Loan saved = loanRepository.save(loan);
        auditService.record(ENTITY_NAME, id, AuditAction.CANCEL, AuditDetails.of(
                "status", AuditDetails.oldNew(previous, saved.getStatus()), "reason", reason));

        Equipment equipment = equipmentRepository.findByIdForUpdate(loan.getEquipment().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento", loan.getEquipment().getId()));
        if (equipment.getStatus() == EquipmentStatus.EMPRESTADO) {
            equipment.cancelLoan();
            Equipment released = equipmentRepository.save(equipment);
            auditService.record(EQUIPMENT_ENTITY, released.getId(), AuditAction.STATUS_CHANGE, AuditDetails.of(
                    "status", AuditDetails.oldNew(EquipmentStatus.EMPRESTADO, released.getStatus()), "loanId", id));
            events.publishEvent(new EquipmentAvailableEvent(released.getId(), released.getType()));
        }
        return detail(find(id));
    }

    /** RN41 — correção por lançamento de ajuste (Admin + motivo), preservado na auditoria. */
    @Transactional
    public AdjustmentResponse addAdjustment(Long id, AdjustmentRequest request) {
        find(id);
        AuditLog log = auditService.record(ENTITY_NAME, id, AuditAction.ADJUSTMENT,
                AuditDetails.of("description", request.description(), "reason", request.reason()));
        return new AdjustmentResponse(log.getId(), request.description(), request.reason(), log.getUsername(),
                log.getOccurredAt());
    }

    @Transactional(readOnly = true)
    public List<AdjustmentResponse> findAdjustments(Long id) {
        return auditService.search(ENTITY_NAME, String.valueOf(id), null, null).stream()
                .filter(log -> log.getAction() == AuditAction.ADJUSTMENT)
                .map(this::toAdjustment)
                .toList();
    }

    // ---------- apoio a outros módulos ----------

    /** Resposta resumida (sem renovações/devolução). */
    public LoanResponse toResponse(Loan loan) {
        return LoanResponse.from(loan, LocalDate.now(clock), parameterService.renewalWindowDays());
    }

    /** Link wa.me com lembrete de vencimento/atraso pronto (ou {@code null} sem telefone/encerrado). */
    public String reminderLink(Loan loan) {
        Beneficiary beneficiary = loan.getBeneficiary();
        if (beneficiary == null || beneficiary.getPhone() == null || beneficiary.getPhone().isBlank()
                || !loan.isOpen()) {
            return null;
        }
        LocalDate today = LocalDate.now(clock);
        Equipment equipment = loan.getEquipment();
        String description = equipment != null && equipment.getType() != null
                ? equipment.getType().getDescription() : "ortopédico";
        String patrimony = equipment != null ? equipment.getPatrimonyNumber() : null;
        boolean renewable = equipment != null && equipment.getType() != null
                && loan.canRenew(today, renewalPolicy(), waitlistService.hasWaitingFor(equipment.getType()));
        String message = loan.isOverdue(today)
                ? whatsApp.overdueMessage(beneficiary.getFullName(), description, patrimony,
                loan.getExpectedReturnDate())
                : whatsApp.dueDateReminderMessage(beneficiary.getFullName(), description, patrimony,
                loan.getExpectedReturnDate(), renewable);
        try {
            return whatsApp.buildLink(beneficiary.getPhone(), message);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public RenewalPolicy renewalPolicy() {
        return new RenewalPolicy(parameterService.maxRenewals(), parameterService.renewalWindowDays());
    }

    // ---------- internos ----------

    private LoanResponse detail(Loan loan) {
        LoanResponse response = toResponse(loan);
        response.setRenewals(renewalRepository.findByLoanId(loan.getId()).stream()
                .map(LoanRenewalResponse::from).toList());
        response.setReturnInfo(returnRepository.findByLoanId(loan.getId()).map(LoanReturnResponse::from).orElse(null));
        response.setAdjustments(findAdjustments(loan.getId()));
        if (loan.isOpen() && loan.getEquipment() != null) {
            List<String> blockers = loan.renewalBlockers(LocalDate.now(clock), renewalPolicy(),
                    waitlistService.hasWaitingFor(loan.getEquipment().getType()));
            response.setCanRenew(blockers.isEmpty());
            response.setRenewalBlockers(blockers);
            response.setWhatsappLink(reminderLink(loan));
        }
        return response;
    }

    private void ensureLendable(Equipment equipment, Long beneficiaryId) {
        String label = equipment.getPatrimonyNumber();
        Map<String, Object> details = AuditDetails.of("equipmentId", equipment.getId(), "status", equipment.getStatus());
        switch (equipment.getStatus()) {
            case DISPONIVEL -> {
                // ok
            }
            case RESERVADO -> {
                if (!equipment.isReservedFor(beneficiaryId)) {
                    throw new ConflictException("Equipamento " + label
                            + " está reservado para outro beneficiário da fila de espera.", details);
                }
            }
            case EMPRESTADO -> throw new ConflictException("Equipamento " + label + " já está emprestado.", details);
            case EM_MANUTENCAO -> throw new ConflictException("Equipamento " + label
                    + " está em manutenção e não pode ser emprestado.", details);
            case BAIXADO -> throw new ConflictException("Equipamento " + label
                    + " foi baixado e não pode ser emprestado.", details);
        }
    }

    private void ensurePendingSignature(Loan loan) {
        if (loan.getStatus() != LoanStatus.PENDENTE_ASSINATURA) {
            throw new BusinessException("Só é possível registrar assinatura de empréstimo pendente de assinatura "
                    + "(status atual: " + loan.getStatus() + ").");
        }
    }

    /** RN06 — quando exige responsável legal, quem assina deve ser o responsável. */
    private String resolveSigner(Loan loan, String informed) {
        LocalDate today = LocalDate.now(clock);
        Beneficiary beneficiary = loan.getBeneficiary();
        String expected = beneficiary.signatoryName(today);
        String signer = informed == null || informed.isBlank() ? expected : informed.trim();
        if (signer == null || signer.isBlank()) {
            throw new BusinessException("Informe o nome de quem assina o termo.");
        }
        if (beneficiary.requiresLegalGuardian(today) && !sameName(signer, expected)) {
            throw new BusinessException("Beneficiário menor de idade ou legalmente incapaz: o termo deve ser assinado "
                    + "pelo responsável legal (" + expected + ").");
        }
        return signer;
    }

    private void auditSignature(Loan saved) {
        auditService.record(ENTITY_NAME, saved.getId(), AuditAction.STATUS_CHANGE, AuditDetails.of(
                "status", AuditDetails.oldNew(LoanStatus.PENDENTE_ASSINATURA, saved.getStatus()),
                "signatureType", saved.getSignatureType(),
                "signedByName", saved.getSignedByName(),
                "signedAt", saved.getSignedAt()));
    }

    private static String signedFileExtension(String contentType, String originalFileName) {
        if (contentType != null && SIGNED_FILE_TYPES.containsKey(contentType.toLowerCase(Locale.ROOT))) {
            return SIGNED_FILE_TYPES.get(contentType.toLowerCase(Locale.ROOT));
        }
        if (originalFileName != null && originalFileName.contains(".")) {
            String extension = originalFileName.substring(originalFileName.lastIndexOf('.') + 1)
                    .toLowerCase(Locale.ROOT);
            if (extension.equals("jpeg")) {
                return "jpg";
            }
            if (SIGNED_FILE_TYPES.containsValue(extension)) {
                return extension;
            }
        }
        throw new BusinessException("Formato do termo assinado não suportado. Envie PDF, JPG ou PNG.");
    }

    private AdjustmentResponse toAdjustment(AuditLog log) {
        String description = null;
        String reason = null;
        if (log.getDetails() != null) {
            try {
                JsonNode node = objectMapper.readTree(log.getDetails());
                description = node.path("description").asText(null);
                reason = node.path("reason").asText(null);
            } catch (Exception e) {
                description = log.getDetails();
            }
        }
        return new AdjustmentResponse(log.getId(), description, reason, log.getUsername(), log.getOccurredAt());
    }

    private Loan find(Long id) {
        return loanRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Empréstimo", id));
    }

    private static boolean sameName(String a, String b) {
        return normalize(a).equals(normalize(b));
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
