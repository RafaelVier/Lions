package org.lions.backend.application.service.order;

import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.request.LoanCreateRequest;
import org.lions.backend.application.dto.request.LoanOrderApprovalRequest;
import org.lions.backend.application.dto.request.LoanOrderCreateRequest;
import org.lions.backend.application.dto.request.WaitlistEnqueueRequest;
import org.lions.backend.application.dto.response.LoanOrderCreatedResponse;
import org.lions.backend.application.dto.response.LoanOrderDecisionResponse;
import org.lions.backend.application.dto.response.LoanOrderResponse;
import org.lions.backend.application.dto.response.LoanResponse;
import org.lions.backend.application.dto.response.WaitlistEntryResponse;
import org.lions.backend.application.port.CurrentUserPort;
import org.lions.backend.application.service.audit.AuditService;
import org.lions.backend.application.service.beneficiary.BeneficiaryService;
import org.lions.backend.application.service.loan.LoanService;
import org.lions.backend.application.service.notification.NotificationService;
import org.lions.backend.application.service.waitlist.WaitlistService;
import org.lions.backend.application.support.AuditDetails;
import org.lions.backend.domain.entity.Beneficiary;
import org.lions.backend.domain.entity.LoanOrder;
import org.lions.backend.domain.enums.AuditAction;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.NotificationType;
import org.lions.backend.domain.enums.OrderStatus;
import org.lions.backend.domain.enums.WaitlistStatus;
import org.lions.backend.domain.exception.BusinessException;
import org.lions.backend.domain.exception.ResourceNotFoundException;
import org.lions.backend.domain.repository.BeneficiaryRepositoryPort;
import org.lions.backend.domain.repository.LoanOrderRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Solicitações (pedido público em 3 etapas ou cadastro assistido): criação vinculando/criando o beneficiário
 * (RN04–RN07), aprovação (→ empréstimo), rejeição, envio à fila de espera e cancelamento.
 */
@Service
@RequiredArgsConstructor
public class LoanOrderService {

    public static final String ENTITY_NAME = "LoanOrder";
    public static final String PUBLIC_USERNAME = "PUBLICO";

    private final LoanOrderRepositoryPort orderRepository;
    private final BeneficiaryRepositoryPort beneficiaryRepository;
    private final BeneficiaryService beneficiaryService;
    private final LoanService loanService;
    private final WaitlistService waitlistService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final CurrentUserPort currentUser;
    private final Clock clock;

    /**
     * Cria a solicitação (status PENDENTE_ANALISE). Anônimo: exige os dados do beneficiário; se já houver beneficiário
     * ativo com o documento, vincula sem alterar o cadastro. Autenticado (cadastro assistido): pode usar
     * {@code beneficiaryId}. A resposta não contém dados pessoais.
     */
    @Transactional
    public LoanOrderCreatedResponse create(LoanOrderCreateRequest request) {
        boolean authenticated = currentUser.isAuthenticated();
        String actor = authenticated ? currentUser.username() : PUBLIC_USERNAME;

        Beneficiary beneficiary;
        if (authenticated && request.beneficiaryId() != null) {
            beneficiary = beneficiaryRepository.findById(request.beneficiaryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Beneficiário", request.beneficiaryId()));
            if (!beneficiary.isActive()) {
                throw new BusinessException("Beneficiário inativo não pode receber novas solicitações.");
            }
        } else {
            if (request.beneficiary() == null) {
                throw new BusinessException("Informe os dados do beneficiário.");
            }
            if (!Boolean.TRUE.equals(request.beneficiary().consentGranted())) {
                throw new BusinessException("É necessário o consentimento (LGPD) para registrar a solicitação.");
            }
            beneficiary = beneficiaryService.findActiveByCpf(request.beneficiary().cpf())
                    .orElseGet(() -> beneficiaryService.register(request.beneficiary(),
                            authenticated ? null : PUBLIC_USERNAME));
        }

        boolean forSelf = request.forSelf() == null || request.forSelf();
        LoanOrder order = LoanOrder.builder()
                .beneficiary(beneficiary)
                .forSelf(forSelf)
                .requesterName(forSelf ? null : blankToNull(request.requesterName()))
                .requesterRelationship(forSelf ? null : blankToNull(request.requesterRelationship()))
                .equipmentType(request.equipmentType())
                .needReason(blankToNull(request.needReason()))
                .status(OrderStatus.PENDENTE_ANALISE)
                .assistedRegistration(authenticated)
                .build();
        order.validate();
        LoanOrder saved = orderRepository.save(order);

        auditService.recordAs(actor, ENTITY_NAME, saved.getId(), AuditAction.CREATE, AuditDetails.of(
                "beneficiaryId", beneficiary.getId(), "equipmentType", saved.getEquipmentType(),
                "forSelf", forSelf, "assistedRegistration", authenticated));
        notificationService.notifyAdmins(NotificationType.GERAL, "Nova solicitação #" + saved.getId() + ": "
                + saved.getEquipmentType().getDescription()
                + (authenticated ? " (cadastro assistido por " + actor + ")." : " (pedido público)."));
        return new LoanOrderCreatedResponse(saved.getId(), saved.getStatus(), saved.getStatus().getDescription(),
                saved.getCreatedAt(),
                "Solicitação registrada! A equipe do Lions Clube vai analisar o pedido e entrar em contato pelo "
                        + "telefone informado.");
    }

    @Transactional(readOnly = true)
    public List<LoanOrderResponse> findAll(OrderStatus status, EquipmentType equipmentType, Long beneficiaryId) {
        List<LoanOrder> orders;
        if (beneficiaryId != null) {
            orders = orderRepository.findByBeneficiaryId(beneficiaryId);
        } else if (status != null) {
            orders = orderRepository.findByStatus(status);
        } else {
            orders = orderRepository.findAll();
        }
        return orders.stream()
                .filter(o -> status == null || o.getStatus() == status)
                .filter(o -> equipmentType == null || o.getEquipmentType() == equipmentType)
                .sorted(Comparator.comparing(LoanOrder::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(LoanOrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public LoanOrderResponse findById(Long id) {
        return LoanOrderResponse.from(find(id));
    }

    /** Aprova registrando o empréstimo do equipamento escolhido (PENDENTE_ANALISE ou EM_FILA_ESPERA → APROVADO). */
    @Transactional
    public LoanOrderDecisionResponse approve(Long id, LoanOrderApprovalRequest request) {
        LoanOrder order = find(id);
        if (order.getStatus() != OrderStatus.PENDENTE_ANALISE && order.getStatus() != OrderStatus.EM_FILA_ESPERA) {
            throw new BusinessException("Só é possível aprovar solicitações pendentes de análise ou na fila de espera "
                    + "(status atual: " + order.getStatus().getDescription() + ").");
        }
        LoanResponse loan = loanService.registerLoan(LoanCreateRequest.builder()
                .beneficiaryId(order.getBeneficiary().getId())
                .equipmentId(request.equipmentId())
                .orderId(id)
                .loanDate(request.loanDate())
                .retroactiveJustification(request.retroactiveJustification())
                .deliveryNotes(request.deliveryNotes())
                .deliveryPhotoUrl(request.deliveryPhotoUrl())
                .build());
        return new LoanOrderDecisionResponse(LoanOrderResponse.from(find(id)), loan, null);
    }

    @Transactional
    public LoanOrderResponse reject(Long id, String reason) {
        LoanOrder order = find(id);
        OrderStatus previous = order.getStatus();
        order.reject(reason, currentUser.username(), LocalDateTime.now(clock));
        LoanOrder saved = orderRepository.save(order);
        auditService.record(ENTITY_NAME, id, AuditAction.STATUS_CHANGE, AuditDetails.of(
                "status", AuditDetails.oldNew(previous, saved.getStatus()), "reason", reason));
        return LoanOrderResponse.from(saved);
    }

    /** Sem item disponível: envia para a fila do tipo (→ {@code WaitlistService.enqueue}); status EM_FILA_ESPERA. */
    @Transactional
    public LoanOrderDecisionResponse sendToWaitlist(Long id) {
        LoanOrder order = find(id);
        OrderStatus previous = order.getStatus();
        order.sendToWaitlist(currentUser.username(), LocalDateTime.now(clock));
        orderRepository.save(order);
        WaitlistEntryResponse entry = waitlistService.enqueue(WaitlistEnqueueRequest.builder()
                .beneficiaryId(order.getBeneficiary().getId())
                .equipmentType(order.getEquipmentType())
                .orderId(id)
                .build());
        auditService.record(ENTITY_NAME, id, AuditAction.STATUS_CHANGE, AuditDetails.of(
                "status", AuditDetails.oldNew(previous, OrderStatus.EM_FILA_ESPERA),
                "waitlistEntryId", entry.getId()));
        return new LoanOrderDecisionResponse(LoanOrderResponse.from(find(id)), null, entry);
    }

    /** Cancela (desistência). Se estiver na fila, a posição é removida e a reserva (se houver) liberada. */
    @Transactional
    public LoanOrderResponse cancel(Long id, String reason) {
        LoanOrder order = find(id);
        if (order.getStatus() == OrderStatus.EM_FILA_ESPERA) {
            waitlistService.findByBeneficiary(order.getBeneficiary().getId()).stream()
                    .filter(entry -> id.equals(entry.getOrderId()))
                    .filter(entry -> entry.getStatus() == WaitlistStatus.AGUARDANDO
                            || entry.getStatus() == WaitlistStatus.RESERVADO)
                    .forEach(entry -> waitlistService.remove(entry.getId(), "Solicitação cancelada: " + reason));
            order = find(id);
        }
        if (order.getStatus() != OrderStatus.CANCELADO) {
            OrderStatus previous = order.getStatus();
            order.cancel(reason, currentUser.username(), LocalDateTime.now(clock));
            order = orderRepository.save(order);
            auditService.record(ENTITY_NAME, id, AuditAction.CANCEL, AuditDetails.of(
                    "status", AuditDetails.oldNew(previous, order.getStatus()), "reason", reason));
        }
        return LoanOrderResponse.from(order);
    }

    private LoanOrder find(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Solicitação", id));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
