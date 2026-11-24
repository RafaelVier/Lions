package org.lions.backend.application.service.waitlist;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lions.backend.application.dto.request.WaitlistEnqueueRequest;
import org.lions.backend.application.dto.request.WaitlistPriorityRequest;
import org.lions.backend.application.dto.response.BeneficiaryRestriction;
import org.lions.backend.application.dto.response.WaitlistEntryResponse;
import org.lions.backend.application.event.BeneficiaryRestrictedEvent;
import org.lions.backend.application.event.EquipmentAvailableEvent;
import org.lions.backend.application.port.CurrentUserPort;
import org.lions.backend.application.service.audit.AuditService;
import org.lions.backend.application.service.loan.BeneficiaryRestrictionService;
import org.lions.backend.application.service.notification.NotificationService;
import org.lions.backend.application.service.parameter.ParameterService;
import org.lions.backend.application.support.AuditDetails;
import org.lions.backend.application.support.WhatsAppMessageHelper;
import org.lions.backend.domain.entity.Beneficiary;
import org.lions.backend.domain.entity.Equipment;
import org.lions.backend.domain.entity.LoanOrder;
import org.lions.backend.domain.entity.WaitlistEntry;
import org.lions.backend.domain.enums.AuditAction;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.NotificationType;
import org.lions.backend.domain.enums.OrderStatus;
import org.lions.backend.domain.enums.WaitlistStatus;
import org.lions.backend.domain.exception.BusinessException;
import org.lions.backend.domain.exception.ConflictException;
import org.lions.backend.domain.exception.ResourceNotFoundException;
import org.lions.backend.domain.repository.BeneficiaryRepositoryPort;
import org.lions.backend.domain.repository.EquipmentRepositoryPort;
import org.lions.backend.domain.repository.LoanOrderRepositoryPort;
import org.lions.backend.domain.repository.LoanRepositoryPort;
import org.lions.backend.domain.repository.WaitlistEntryRepositoryPort;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Fila de espera por tipo de equipamento (RN29–RN34). Quando um item fica disponível ({@link EquipmentAvailableEvent})
 * ele é reservado para o primeiro da fila por {@code waitlist.reservation-hours} (RN32); reserva expirada/recusada
 * passa ao próximo e o beneficiário volta ao fim da fila, sendo removido após N vezes consecutivas (RN33).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WaitlistService {

    public static final String ENTITY_NAME = "WaitlistEntry";
    public static final String RULE_RESERVATION = "RN32";
    public static final String RULE_EXPIRATION = "RN33";
    public static final String RULE_RESTRICTION = "RN34";
    public static final String EVENT_EXPIRED = "RESERVA_EXPIRADA";

    private final WaitlistEntryRepositoryPort waitlistRepository;
    private final BeneficiaryRepositoryPort beneficiaryRepository;
    private final EquipmentRepositoryPort equipmentRepository;
    private final LoanOrderRepositoryPort orderRepository;
    private final LoanRepositoryPort loanRepository;
    private final BeneficiaryRestrictionService restrictionService;
    private final NotificationService notificationService;
    private final ParameterService parameterService;
    private final WhatsAppMessageHelper whatsApp;
    private final AuditService auditService;
    private final CurrentUserPort currentUser;
    private final Clock clock;

    // ---------- contrato §6.2 ----------

    /** Inclui o beneficiário na fila do tipo (RN29, RN31, RN34). */
    @Transactional
    public WaitlistEntryResponse enqueue(WaitlistEnqueueRequest req) {
        Beneficiary beneficiary = beneficiaryRepository.findById(req.getBeneficiaryId())
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiário", req.getBeneficiaryId()));
        EquipmentType type = req.getEquipmentType();
        BeneficiaryRestriction restriction = restrictionService.evaluate(beneficiary.getId());
        if (restriction.blocked()) {
            throw new BusinessException("Beneficiário não pode entrar na fila de espera: "
                    + String.join(" ", restriction.reasons()));
        }
        if (waitlistRepository.existsActiveByBeneficiaryIdAndType(beneficiary.getId(), type)) {
            Long existingId = waitlistRepository.findActiveByBeneficiaryId(beneficiary.getId()).stream()
                    .filter(entry -> entry.getEquipmentType() == type)
                    .map(WaitlistEntry::getId)
                    .findFirst().orElse(null);
            throw new ConflictException("Beneficiário já possui posição ativa na fila de " + type.getDescription() + ".",
                    AuditDetails.of("existingId", existingId));
        }
        boolean hasOpenLoanOfType = loanRepository.findOpenByBeneficiaryId(beneficiary.getId()).stream()
                .anyMatch(loan -> loan.getEquipment() != null && loan.getEquipment().getType() == type);
        if (hasOpenLoanOfType) {
            throw new BusinessException("Beneficiário já possui empréstimo em aberto de " + type.getDescription() + ".");
        }

        LocalDateTime now = now();
        LocalDateTime requestedAt = now;
        if (req.getOrderId() != null) {
            LoanOrder order = orderRepository.findById(req.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Solicitação", req.getOrderId()));
            if (!Objects.equals(order.getBeneficiary().getId(), beneficiary.getId())) {
                throw new BusinessException("A solicitação informada não pertence ao beneficiário.");
            }
            if (order.getEquipmentType() != type) {
                throw new BusinessException("O tipo de equipamento difere do tipo da solicitação.");
            }
            if (order.getCreatedAt() != null) {
                requestedAt = order.getCreatedAt();
            }
        }

        WaitlistEntry saved = waitlistRepository.save(WaitlistEntry.builder()
                .beneficiary(beneficiary)
                .equipmentType(type)
                .orderId(req.getOrderId())
                .status(WaitlistStatus.AGUARDANDO)
                .queuedAt(now)
                .originalRequestedAt(requestedAt)
                .createdBy(currentUser.username())
                .build());
        auditService.record(ENTITY_NAME, saved.getId(), AuditAction.CREATE, AuditDetails.of(
                "beneficiaryId", beneficiary.getId(), "equipmentType", type, "orderId", req.getOrderId()));

        // Itens já disponíveis do tipo são reservados para a fila imediatamente (RN32)
        for (Equipment available : equipmentRepository.findByTypeAndStatus(type, EquipmentStatus.DISPONIVEL)) {
            if (allocate(available.getId(), type, null).isEmpty()) {
                break;
            }
        }
        return toResponse(find(saved.getId()));
    }

    /** Há alguém AGUARDANDO para o tipo (bloqueia renovação — RN23). */
    @Transactional(readOnly = true)
    public boolean hasWaitingFor(EquipmentType type) {
        return waitlistRepository.countActiveByType(type) > 0;
    }

    /** O beneficiário tem posição ativa (AGUARDANDO/RESERVADO) em alguma fila. */
    @Transactional(readOnly = true)
    public boolean hasActiveEntry(Long beneficiaryId) {
        return !waitlistRepository.findActiveByBeneficiaryId(beneficiaryId).isEmpty();
    }

    /**
     * Marca como ATENDIDO a posição do beneficiário para o tipo ao registrar o empréstimo (RN32). Se ele tinha reserva de
     * outro item do mesmo tipo, a reserva é liberada e o item segue para o próximo da fila.
     */
    @Transactional
    public void markServed(Long beneficiaryId, Long equipmentId, EquipmentType type) {
        LocalDateTime now = now();
        for (WaitlistEntry entry : waitlistRepository.findActiveByBeneficiaryId(beneficiaryId)) {
            if (entry.getEquipmentType() != type) {
                continue;
            }
            Long reservedEquipmentId = entry.getReservedEquipmentId();
            entry.serve(now);
            waitlistRepository.save(entry);
            auditService.recordWithRule(ENTITY_NAME, entry.getId(), AuditAction.STATUS_CHANGE, RULE_RESERVATION,
                    AuditDetails.of("status", WaitlistStatus.ATENDIDO, "equipmentId", equipmentId));
            approveLinkedOrder(entry, now);
            if (reservedEquipmentId != null && !reservedEquipmentId.equals(equipmentId)) {
                releaseReservation(reservedEquipmentId, beneficiaryId)
                        .ifPresent(released -> allocate(released.getId(), released.getType(), entry.getId()));
            }
        }
    }

    // ---------- consultas ----------

    /** Sem {@code status}: posições ativas (AGUARDANDO e RESERVADO), na ordem efetiva da fila (RN29/RN30). */
    @Transactional(readOnly = true)
    public List<WaitlistEntryResponse> findAll(EquipmentType type, WaitlistStatus status) {
        List<WaitlistEntry> entries = new ArrayList<>();
        if (status != null) {
            entries.addAll(waitlistRepository.findByStatus(status));
        } else {
            entries.addAll(waitlistRepository.findByStatus(WaitlistStatus.RESERVADO));
            entries.addAll(waitlistRepository.findByStatus(WaitlistStatus.AGUARDANDO));
        }
        Map<Long, Integer> positions = positions();
        return entries.stream()
                .filter(entry -> type == null || entry.getEquipmentType() == type)
                .sorted(Comparator
                        .comparing((WaitlistEntry e) -> e.getEquipmentType().ordinal())
                        .thenComparing(e -> e.getStatus() == WaitlistStatus.RESERVADO ? 0 : 1)
                        .thenComparing(e -> positions.getOrDefault(e.getId(), Integer.MAX_VALUE))
                        .thenComparing(WaitlistEntry::getQueuedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(entry -> toResponse(entry, positions))
                .toList();
    }

    @Transactional(readOnly = true)
    public WaitlistEntryResponse findById(Long id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public List<WaitlistEntryResponse> findByBeneficiary(Long beneficiaryId) {
        Map<Long, Integer> positions = positions();
        return waitlistRepository.findByBeneficiaryId(beneficiaryId).stream()
                .map(entry -> toResponse(entry, positions))
                .toList();
    }

    // ---------- casos de uso ----------

    /** RN30 — prioridade (somente Admin, verificado no controller) com motivo e justificativa. */
    @Transactional
    public WaitlistEntryResponse setPriority(Long id, WaitlistPriorityRequest request) {
        WaitlistEntry entry = find(id);
        WaitlistEntry before = find(id);
        entry.setPriority(request.reason(), request.justification(), currentUser.username());
        waitlistRepository.save(entry);
        auditService.record(ENTITY_NAME, id, AuditAction.UPDATE, auditService.diff(before, entry,
                "priority", "priorityReason", "priorityJustification", "priorityAuthorizedBy"));
        return toResponse(find(id));
    }

    @Transactional
    public WaitlistEntryResponse clearPriority(Long id) {
        WaitlistEntry entry = find(id);
        WaitlistEntry before = find(id);
        if (!entry.isPriority()) {
            throw new BusinessException("A posição não possui prioridade.");
        }
        entry.clearPriority();
        waitlistRepository.save(entry);
        auditService.record(ENTITY_NAME, id, AuditAction.UPDATE, auditService.diff(before, entry,
                "priority", "priorityReason", "priorityJustification", "priorityAuthorizedBy"));
        return toResponse(find(id));
    }

    /** RN33 — recusa da reserva pelo beneficiário: item vai ao próximo; ele volta ao fim (ou é removido). */
    @Transactional
    public WaitlistEntryResponse decline(Long id) {
        WaitlistEntry entry = find(id);
        Long equipmentId = entry.getReservedEquipmentId();
        LocalDateTime now = now();
        boolean removed = entry.expireOrDecline(now, parameterService.maxConsecutiveExpirations());
        waitlistRepository.save(entry);
        auditService.recordWithRule(ENTITY_NAME, id, AuditAction.STATUS_CHANGE, RULE_EXPIRATION,
                AuditDetails.of("event", "RESERVA_RECUSADA", "equipmentId", equipmentId, "removed", removed));
        if (removed) {
            cancelLinkedOrder(entry, WaitlistEntry.REMOVED_BY_EXPIRATIONS, now);
        }
        releaseReservation(equipmentId, entry.getBeneficiary().getId())
                .ifPresent(released -> allocate(released.getId(), released.getType(), entry.getId()));
        return toResponse(find(id));
    }

    /** Remoção manual (desistência etc.). Libera a reserva, se houver. */
    @Transactional
    public WaitlistEntryResponse remove(Long id, String reason) {
        WaitlistEntry entry = find(id);
        removeEntry(entry, reason, false);
        return toResponse(find(id));
    }

    /** RN33 — rotina: expira reservas vencidas. Retorna quantas foram expiradas. */
    @Transactional
    public int expireReservations() {
        LocalDateTime now = now();
        int maxConsecutive = parameterService.maxConsecutiveExpirations();
        int expired = 0;
        for (WaitlistEntry entry : waitlistRepository.findExpiredReservations(now)) {
            Long equipmentId = entry.getReservedEquipmentId();
            boolean removed = entry.expireOrDecline(now, maxConsecutive);
            waitlistRepository.save(entry);
            auditService.recordAutomatic(ENTITY_NAME, entry.getId(), AuditAction.STATUS_CHANGE, RULE_EXPIRATION,
                    AuditDetails.of("event", EVENT_EXPIRED, "equipmentId", equipmentId, "removed", removed));
            if (removed) {
                cancelLinkedOrder(entry, WaitlistEntry.REMOVED_BY_EXPIRATIONS, now);
            }
            releaseReservation(equipmentId, entry.getBeneficiary().getId())
                    .ifPresent(released -> allocate(released.getId(), released.getType(), entry.getId()));
            expired++;
        }
        return expired;
    }

    // ---------- eventos ----------

    /** RN32 — equipamento disponível: reserva para o primeiro da fila do tipo. */
    @EventListener
    @Transactional
    public void onEquipmentAvailable(EquipmentAvailableEvent event) {
        allocate(event.equipmentId(), event.type(), null);
    }

    /** RN34 — beneficiário inativado/anonimizado/bloqueado: remove as posições ativas. */
    @EventListener
    @Transactional
    public void onBeneficiaryRestricted(BeneficiaryRestrictedEvent event) {
        for (WaitlistEntry entry : waitlistRepository.findActiveByBeneficiaryId(event.beneficiaryId())) {
            String reason = event.reason() == null || event.reason().isBlank()
                    ? "Beneficiário bloqueado ou inativado."
                    : event.reason();
            removeEntry(entry, "Removido automaticamente: " + reason, true);
        }
    }

    // ---------- internos ----------

    /**
     * Reserva o equipamento (se ainda DISPONIVEL) para o primeiro da fila do tipo, ignorando {@code excludeEntryId}.
     * Beneficiários bloqueados (RN20) ou inativos são removidos da fila (RN34).
     */
    private Optional<WaitlistEntry> allocate(Long equipmentId, EquipmentType type, Long excludeEntryId) {
        if (equipmentId == null) {
            return Optional.empty();
        }
        Optional<Equipment> locked = equipmentRepository.findByIdForUpdate(equipmentId);
        if (locked.isEmpty() || !locked.get().isAvailable()) {
            return Optional.empty();
        }
        Equipment equipment = locked.get();
        LocalDateTime now = now();
        for (WaitlistEntry entry : waitlistRepository.findActiveByType(type)) {
            if (entry.getId().equals(excludeEntryId)) {
                continue;
            }
            Beneficiary beneficiary = entry.getBeneficiary();
            BeneficiaryRestriction restriction = restrictionService.evaluate(beneficiary.getId());
            if (restriction.removesFromWaitlist()) {
                removeEntry(entry, "Removido automaticamente: " + String.join(" ", restriction.reasons()), true);
                continue;
            }
            if (restriction.blocked()) {
                continue;
            }
            equipment.reserveFor(beneficiary.getId());
            Equipment reserved = equipmentRepository.save(equipment);
            entry.reserve(reserved.getId(), now, parameterService.reservationHours());
            WaitlistEntry saved = waitlistRepository.save(entry);
            auditService.recordAutomatic(ENTITY_NAME, saved.getId(), AuditAction.STATUS_CHANGE, RULE_RESERVATION,
                    AuditDetails.of("event", "RESERVA", "equipmentId", reserved.getId(),
                            "patrimonyNumber", reserved.getPatrimonyNumber(),
                            "expiresAt", saved.getReservationExpiresAt()));
            auditService.recordAutomatic("Equipment", reserved.getId(), AuditAction.STATUS_CHANGE, RULE_RESERVATION,
                    AuditDetails.of("status", AuditDetails.oldNew(EquipmentStatus.DISPONIVEL, EquipmentStatus.RESERVADO),
                            "beneficiaryId", beneficiary.getId()));
            notificationService.notifyBeneficiary(beneficiary.getId(), null, NotificationType.RESERVA_DISPONIVEL,
                    whatsApp.reservationAvailableMessage(beneficiary.getFullName(), type.getDescription(),
                            reserved.getPatrimonyNumber(), saved.getReservationExpiresAt()));
            return Optional.of(saved);
        }
        return Optional.empty();
    }

    private void removeEntry(WaitlistEntry entry, String reason, boolean automatic) {
        Long equipmentId = entry.getReservedEquipmentId();
        LocalDateTime now = now();
        entry.remove(reason, now);
        waitlistRepository.save(entry);
        if (automatic) {
            auditService.recordAutomatic(ENTITY_NAME, entry.getId(), AuditAction.STATUS_CHANGE, RULE_RESTRICTION,
                    AuditDetails.of("status", WaitlistStatus.REMOVIDO, "reason", reason));
        } else {
            auditService.record(ENTITY_NAME, entry.getId(), AuditAction.STATUS_CHANGE,
                    AuditDetails.of("status", WaitlistStatus.REMOVIDO, "reason", reason));
        }
        cancelLinkedOrder(entry, reason, now);
        if (equipmentId != null) {
            releaseReservation(equipmentId, entry.getBeneficiary().getId())
                    .ifPresent(released -> allocate(released.getId(), released.getType(), entry.getId()));
        }
    }

    /** RESERVADO → DISPONIVEL se o item ainda estiver reservado para o beneficiário. */
    private Optional<Equipment> releaseReservation(Long equipmentId, Long beneficiaryId) {
        if (equipmentId == null) {
            return Optional.empty();
        }
        Optional<Equipment> locked = equipmentRepository.findByIdForUpdate(equipmentId);
        if (locked.isEmpty() || !locked.get().isReservedFor(beneficiaryId)) {
            return Optional.empty();
        }
        Equipment equipment = locked.get();
        equipment.releaseReservation();
        Equipment saved = equipmentRepository.save(equipment);
        auditService.recordAutomatic("Equipment", saved.getId(), AuditAction.STATUS_CHANGE, RULE_EXPIRATION,
                AuditDetails.of("status", AuditDetails.oldNew(EquipmentStatus.RESERVADO, EquipmentStatus.DISPONIVEL)));
        return Optional.of(saved);
    }

    private void cancelLinkedOrder(WaitlistEntry entry, String reason, LocalDateTime now) {
        if (entry.getOrderId() == null) {
            return;
        }
        orderRepository.findById(entry.getOrderId())
                .filter(order -> order.getStatus() == OrderStatus.EM_FILA_ESPERA)
                .ifPresent(order -> {
                    order.cancel(reason, currentUser.username(), now);
                    orderRepository.save(order);
                    auditService.record("LoanOrder", order.getId(), AuditAction.CANCEL,
                            AuditDetails.of("reason", reason, "waitlistEntryId", entry.getId()));
                });
    }

    private void approveLinkedOrder(WaitlistEntry entry, LocalDateTime now) {
        if (entry.getOrderId() == null) {
            return;
        }
        orderRepository.findById(entry.getOrderId())
                .filter(order -> order.getStatus() == OrderStatus.EM_FILA_ESPERA)
                .ifPresent(order -> {
                    order.approve(currentUser.username(), now);
                    orderRepository.save(order);
                    auditService.record("LoanOrder", order.getId(), AuditAction.STATUS_CHANGE,
                            AuditDetails.of("status", OrderStatus.APROVADO, "waitlistEntryId", entry.getId()));
                });
    }

    private WaitlistEntry find(Long id) {
        return waitlistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Posição na fila", id));
    }

    /** Posição efetiva (1 = próximo) de cada entrada AGUARDANDO, por tipo (prioridade desc, queuedAt asc). */
    private Map<Long, Integer> positions() {
        Map<Long, Integer> positions = new HashMap<>();
        Map<EquipmentType, List<WaitlistEntry>> byType = new EnumMap<>(EquipmentType.class);
        for (EquipmentType type : EquipmentType.values()) {
            byType.put(type, waitlistRepository.findActiveByType(type));
        }
        byType.values().forEach(list -> {
            for (int i = 0; i < list.size(); i++) {
                positions.put(list.get(i).getId(), i + 1);
            }
        });
        return positions;
    }

    private WaitlistEntryResponse toResponse(WaitlistEntry entry) {
        return toResponse(entry, positions());
    }

    private WaitlistEntryResponse toResponse(WaitlistEntry entry, Map<Long, Integer> positions) {
        WaitlistEntryResponse response = WaitlistEntryResponse.from(entry);
        if (entry.getStatus() == WaitlistStatus.AGUARDANDO) {
            response.setPosition(positions.get(entry.getId()));
        }
        if (entry.getReservedEquipmentId() != null) {
            equipmentRepository.findById(entry.getReservedEquipmentId())
                    .ifPresent(equipment -> response.setReservedPatrimonyNumber(equipment.getPatrimonyNumber()));
        }
        return response;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
