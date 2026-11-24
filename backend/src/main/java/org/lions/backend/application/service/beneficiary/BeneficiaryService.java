package org.lions.backend.application.service.beneficiary;

import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.request.BeneficiaryRequest;
import org.lions.backend.application.dto.request.ConsentRequest;
import org.lions.backend.application.dto.response.BeneficiaryHistoryResponse;
import org.lions.backend.application.dto.response.BeneficiaryResponse;
import org.lions.backend.application.dto.response.BeneficiaryRestriction;
import org.lions.backend.application.dto.response.BeneficiarySummaryResponse;
import org.lions.backend.application.dto.response.LoanOrderResponse;
import org.lions.backend.application.dto.response.LoanRenewalResponse;
import org.lions.backend.application.dto.response.LoanResponse;
import org.lions.backend.application.dto.response.LoanReturnResponse;
import org.lions.backend.application.dto.response.OccurrenceResponse;
import org.lions.backend.application.event.BeneficiaryRestrictedEvent;
import org.lions.backend.application.port.CurrentUserPort;
import org.lions.backend.application.service.audit.AuditService;
import org.lions.backend.application.service.loan.BeneficiaryRestrictionService;
import org.lions.backend.application.service.parameter.ParameterService;
import org.lions.backend.application.service.waitlist.WaitlistService;
import org.lions.backend.application.support.AuditDetails;
import org.lions.backend.application.support.CpfValidator;
import org.lions.backend.domain.entity.Beneficiary;
import org.lions.backend.domain.entity.Loan;
import org.lions.backend.domain.entity.LoanOrder;
import org.lions.backend.domain.enums.AuditAction;
import org.lions.backend.domain.enums.NotificationChannel;
import org.lions.backend.domain.enums.OccurrenceStatus;
import org.lions.backend.domain.exception.BusinessException;
import org.lions.backend.domain.exception.ConflictException;
import org.lions.backend.domain.exception.ResourceNotFoundException;
import org.lions.backend.domain.repository.BeneficiaryRepositoryPort;
import org.lions.backend.domain.repository.LoanOrderRepositoryPort;
import org.lions.backend.domain.repository.LoanRenewalRepositoryPort;
import org.lions.backend.domain.repository.LoanRepositoryPort;
import org.lions.backend.domain.repository.LoanReturnRepositoryPort;
import org.lions.backend.domain.repository.OccurrenceRepositoryPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Beneficiários (RF02/RF09): cadastro com consentimento (RN04–RN07), edição auditada (RN58), inativação (RN08, RN34),
 * reativação (RN04), anonimização (RN59) e histórico consolidado (RN42).
 */
@Service
@RequiredArgsConstructor
public class BeneficiaryService {

    public static final String ENTITY_NAME = "Beneficiary";
    private static final String[] AUDITED_FIELDS = {
            "fullName", "cpf", "birthDate", "phone", "email", "cep", "street", "number",
            "complement", "neighborhood", "city", "state", "alternativeContactName", "alternativeContactPhone",
            "alternativeContactRelationship", "legallyIncapable", "legalGuardianName", "legalGuardianCpf",
            "legalGuardianPhone", "legalGuardianEmail", "legalGuardianRelationship", "notes"};

    private final BeneficiaryRepositoryPort beneficiaryRepository;
    private final LoanRepositoryPort loanRepository;
    private final LoanRenewalRepositoryPort renewalRepository;
    private final LoanReturnRepositoryPort returnRepository;
    private final LoanOrderRepositoryPort orderRepository;
    private final OccurrenceRepositoryPort occurrenceRepository;
    private final WaitlistService waitlistService;
    private final BeneficiaryRestrictionService restrictionService;
    private final ParameterService parameterService;
    private final AuditService auditService;
    private final CurrentUserPort currentUser;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public BeneficiaryResponse create(BeneficiaryRequest request) {
        return BeneficiaryResponse.from(register(request, null), today());
    }

    /**
     * Cadastra novo beneficiário (RN04–RN07). Usado também pelo pedido público; {@code auditUsername} sobrescreve o
     * autor da auditoria (ex.: {@code PUBLICO}).
     */
    @Transactional
    public Beneficiary register(BeneficiaryRequest request, String auditUsername) {
        if (!Boolean.TRUE.equals(request.consentGranted())) {
            throw new BusinessException("O cadastro só pode ser efetivado com o consentimento (LGPD) do beneficiário "
                    + "ou do responsável legal.");
        }
        String cpf = normalizeCpf(request.cpf());
        ensureNoActiveDuplicate(cpf, null);

        LocalDate today = today();
        Beneficiary beneficiary = new Beneficiary();
        apply(beneficiary, request, cpf);
        String givenBy = blankToNull(request.consentGivenBy());
        if (givenBy == null) {
            givenBy = beneficiary.requiresLegalGuardian(today)
                    ? beneficiary.getLegalGuardianName()
                    : beneficiary.getFullName();
        }
        beneficiary.grantConsent(channels(request.notificationChannels()), givenBy, LocalDateTime.now(clock));
        beneficiary.validate(today);

        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        Map<String, Object> details = AuditDetails.of(
                "maskedCpf", saved.getMaskedCpf(), "consentGivenBy", saved.getConsentGivenBy(),
                "notificationChannels", saved.getNotificationChannels());
        if (auditUsername != null) {
            auditService.recordAs(auditUsername, ENTITY_NAME, saved.getId(), AuditAction.CREATE, details);
        } else {
            auditService.record(ENTITY_NAME, saved.getId(), AuditAction.CREATE, details);
        }
        return saved;
    }

    /** Beneficiário ativo com o CPF (aceita com ou sem máscara). */
    @Transactional(readOnly = true)
    public Optional<Beneficiary> findActiveByCpf(String cpf) {
        return beneficiaryRepository.findActiveByCpf(normalizeCpf(cpf));
    }

    /** Lista com documento mascarado. Filtros opcionais e combináveis. */
    @Transactional(readOnly = true)
    public List<BeneficiarySummaryResponse> findAll(String name, String document, Boolean active) {
        Stream<Beneficiary> stream;
        if (document != null && !document.isBlank()) {
            stream = beneficiaryRepository.findByCpf(normalizeCpf(document)).stream();
        } else if (name != null && !name.isBlank()) {
            stream = beneficiaryRepository.searchByName(name.trim()).stream();
        } else {
            stream = beneficiaryRepository.findAll(active).stream();
        }
        String nameFilter = name == null ? null : name.trim().toLowerCase(Locale.ROOT);
        Map<Long, Beneficiary> unique = new LinkedHashMap<>();
        stream.filter(b -> active == null || b.isActive() == active)
                .filter(b -> nameFilter == null || nameFilter.isEmpty()
                        || (b.getFullName() != null && b.getFullName().toLowerCase(Locale.ROOT).contains(nameFilter)))
                .forEach(b -> unique.putIfAbsent(b.getId(), b));
        return unique.values().stream()
                .sorted(Comparator.comparing(Beneficiary::getFullName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(BeneficiarySummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BeneficiaryResponse findById(Long id) {
        return BeneficiaryResponse.from(find(id), today());
    }

    /** Correção cadastral (RN59) auditada com valores anteriores (RN58). Consentimento tem endpoint próprio. */
    @Transactional
    public BeneficiaryResponse update(Long id, BeneficiaryRequest request) {
        Beneficiary beneficiary = find(id);
        Beneficiary before = find(id);
        if (beneficiary.isAnonymized()) {
            throw new BusinessException("Beneficiário anonimizado não pode ser editado.");
        }
        String cpf = normalizeCpf(request.cpf());
        if (beneficiary.isActive() && !cpf.equals(beneficiary.getCpf())) {
            ensureNoActiveDuplicate(cpf, id);
        }
        apply(beneficiary, request, cpf);
        beneficiary.validate(today());
        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        var changes = auditService.diff(before, saved, AUDITED_FIELDS);
        if (!changes.isEmpty()) {
            auditService.record(ENTITY_NAME, id, AuditAction.UPDATE, changes);
        }
        return BeneficiaryResponse.from(saved, today());
    }

    /** RN07/RN47 — renova o consentimento e os canais autorizados (data/hora do sistema). */
    @Transactional
    public BeneficiaryResponse updateConsent(Long id, ConsentRequest request) {
        Beneficiary beneficiary = find(id);
        if (beneficiary.isAnonymized()) {
            throw new BusinessException("Beneficiário anonimizado não pode registrar consentimento.");
        }
        Set<NotificationChannel> previousChannels = EnumSet.noneOf(NotificationChannel.class);
        previousChannels.addAll(beneficiary.getNotificationChannels());
        String givenBy = blankToNull(request.consentGivenBy());
        if (givenBy == null) {
            givenBy = beneficiary.signatoryName(today());
        }
        beneficiary.grantConsent(channels(request.notificationChannels()), givenBy, LocalDateTime.now(clock));
        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        auditService.record(ENTITY_NAME, id, AuditAction.UPDATE, AuditDetails.of(
                "notificationChannels", AuditDetails.oldNew(previousChannels, saved.getNotificationChannels()),
                "consentTimestamp", saved.getConsentTimestamp(),
                "consentGivenBy", saved.getConsentGivenBy()));
        return BeneficiaryResponse.from(saved, today());
    }

    /** RN08 — recusa com empréstimo não encerrado, ocorrência aberta ou posição ativa na fila; RN34 — evento. */
    @Transactional
    public BeneficiaryResponse inactivate(Long id, String reason) {
        Beneficiary beneficiary = find(id);
        if (!loanRepository.findOpenByBeneficiaryId(id).isEmpty()) {
            throw new BusinessException("Beneficiário possui empréstimo não encerrado e não pode ser inativado.");
        }
        if (occurrenceRepository.existsByBeneficiaryIdAndStatus(id, OccurrenceStatus.ABERTA)) {
            throw new BusinessException("Beneficiário possui ocorrência aberta e não pode ser inativado.");
        }
        if (waitlistService.hasActiveEntry(id)) {
            throw new BusinessException("Beneficiário possui posição ativa na fila de espera e não pode ser inativado. "
                    + "Remova-o da fila antes.");
        }
        beneficiary.inactivate(reason, LocalDateTime.now(clock));
        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        auditService.record(ENTITY_NAME, id, AuditAction.INACTIVATE, AuditDetails.of(
                "active", AuditDetails.oldNew(true, false), "reason", reason));
        events.publishEvent(new BeneficiaryRestrictedEvent(id, "Beneficiário inativado: " + reason));
        return BeneficiaryResponse.from(saved, today());
    }

    /** RN04 — não pode haver outro beneficiário ativo com o mesmo documento. */
    @Transactional
    public BeneficiaryResponse reactivate(Long id) {
        Beneficiary beneficiary = find(id);
        if (!beneficiary.isAnonymized()) {
            ensureNoActiveDuplicate(beneficiary.getCpf(), id);
        }
        beneficiary.reactivate(LocalDateTime.now(clock));
        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        auditService.record(ENTITY_NAME, id, AuditAction.REACTIVATE, AuditDetails.of(
                "active", AuditDetails.oldNew(false, true)));
        return BeneficiaryResponse.from(saved, today());
    }

    /**
     * RN59 — substitui os dados pessoais (inclusive o motivo de saúde e o solicitante nas solicitações), preservando
     * empréstimos e ocorrências. Não permitido com empréstimo em aberto.
     */
    @Transactional
    public BeneficiaryResponse anonymize(Long id) {
        Beneficiary beneficiary = find(id);
        if (!loanRepository.findOpenByBeneficiaryId(id).isEmpty()) {
            throw new BusinessException("Beneficiário possui empréstimo em aberto e não pode ser anonimizado.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        beneficiary.anonymize(now);
        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        for (LoanOrder order : orderRepository.findByBeneficiaryId(id)) {
            order.setNeedReason(null);
            if (!order.isForSelf()) {
                order.setRequesterName("Anonimizado");
                order.setRequesterRelationship(null);
            }
            orderRepository.save(order);
        }
        auditService.record(ENTITY_NAME, id, AuditAction.ANONYMIZE, AuditDetails.of("anonymizedAt", now));
        events.publishEvent(new BeneficiaryRestrictedEvent(id, "Beneficiário anonimizado (LGPD)."));
        return BeneficiaryResponse.from(saved, today());
    }

    /** RN42 — histórico consolidado, inclusive de equipamentos baixados. */
    @Transactional(readOnly = true)
    public BeneficiaryHistoryResponse history(Long id) {
        Beneficiary beneficiary = find(id);
        LocalDate today = today();
        int dueSoonDays = parameterService.renewalWindowDays();
        List<LoanResponse> loans = loanRepository.findByBeneficiaryId(id).stream()
                .map(loan -> loanDetail(loan, today, dueSoonDays))
                .toList();
        List<LoanOrderResponse> orders = orderRepository.findByBeneficiaryId(id).stream()
                .sorted(Comparator.comparing(LoanOrder::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(LoanOrderResponse::from)
                .toList();
        List<OccurrenceResponse> occurrences = occurrenceRepository.findByBeneficiaryId(id).stream()
                .map(OccurrenceResponse::from)
                .toList();
        BeneficiaryRestriction restriction = restrictionService.evaluate(id);
        return new BeneficiaryHistoryResponse(BeneficiaryResponse.from(beneficiary, today), restriction, loans,
                orders, waitlistService.findByBeneficiary(id), occurrences);
    }

    @Transactional(readOnly = true)
    public BeneficiaryRestriction restriction(Long id) {
        find(id);
        return restrictionService.evaluate(id);
    }

    // ---------- internos ----------

    private LoanResponse loanDetail(Loan loan, LocalDate today, int dueSoonDays) {
        LoanResponse response = LoanResponse.from(loan, today, dueSoonDays);
        response.setRenewals(renewalRepository.findByLoanId(loan.getId()).stream()
                .map(LoanRenewalResponse::from).toList());
        response.setReturnInfo(returnRepository.findByLoanId(loan.getId()).map(LoanReturnResponse::from).orElse(null));
        return response;
    }

    private void apply(Beneficiary b, BeneficiaryRequest r, String cpf) {
        b.setFullName(trim(r.fullName()));
        b.setCpf(cpf);
        b.setBirthDate(r.birthDate());
        b.setPhone(trim(r.phone()));
        b.setEmail(blankToNull(r.email()));
        b.setCep(trim(r.cep()));
        b.setStreet(trim(r.street()));
        b.setNumber(trim(r.number()));
        b.setComplement(blankToNull(r.complement()));
        b.setNeighborhood(trim(r.neighborhood()));
        b.setCity(trim(r.city()));
        b.setState(r.state() == null ? null : r.state().trim().toUpperCase(Locale.ROOT));
        b.setAlternativeContactName(trim(r.alternativeContactName()));
        b.setAlternativeContactPhone(trim(r.alternativeContactPhone()));
        b.setAlternativeContactRelationship(blankToNull(r.alternativeContactRelationship()));
        b.setLegallyIncapable(Boolean.TRUE.equals(r.legallyIncapable()));
        b.setLegalGuardianName(blankToNull(r.legalGuardianName()));
        b.setLegalGuardianCpf(r.legalGuardianCpf() == null || r.legalGuardianCpf().isBlank()
                ? null : normalizeCpf(r.legalGuardianCpf()));
        b.setLegalGuardianPhone(blankToNull(r.legalGuardianPhone()));
        b.setLegalGuardianEmail(blankToNull(r.legalGuardianEmail()));
        b.setLegalGuardianRelationship(blankToNull(r.legalGuardianRelationship()));
        b.setNotes(blankToNull(r.notes()));
    }

    /** RN04 — CPF já cadastrado para beneficiário ativo → 409 com {@code existingId}. */
    private void ensureNoActiveDuplicate(String cpf, Long ignoreId) {
        beneficiaryRepository.findActiveByCpf(cpf)
                .filter(existing -> !Objects.equals(existing.getId(), ignoreId))
                .ifPresent(existing -> {
                    throw new ConflictException("Já existe beneficiário ativo com este CPF.",
                            Map.of("existingId", existing.getId()));
                });
    }

    /** CPF somente com dígitos. */
    public static String normalizeCpf(String cpf) {
        return cpf == null ? null : CpfValidator.normalize(cpf);
    }

    private static Set<NotificationChannel> channels(Set<NotificationChannel> channels) {
        Set<NotificationChannel> result = EnumSet.noneOf(NotificationChannel.class);
        if (channels != null) {
            channels.stream().filter(Objects::nonNull).forEach(result::add);
        }
        return result;
    }

    private Beneficiary find(Long id) {
        return beneficiaryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Beneficiário", id));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
