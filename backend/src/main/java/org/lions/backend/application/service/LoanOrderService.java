package org.lions.backend.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lions.backend.application.dto.request.LoanApprovalRequest;
import org.lions.backend.application.dto.request.LoanOrderCreateRequest;
import org.lions.backend.application.dto.response.LoanOrderResponse;
import org.lions.backend.domain.entity.Equipment;
import org.lions.backend.domain.entity.Loan;
import org.lions.backend.domain.entity.LoanOrder;
import org.lions.backend.domain.entity.Requester;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.OrderStatus;
import org.lions.backend.domain.exception.BusinessException;
import org.lions.backend.domain.exception.ResourceNotFoundException;
import org.lions.backend.domain.repository.EquipmentRepositoryPort;
import org.lions.backend.domain.repository.LoanOrderRepositoryPort;
import org.lions.backend.domain.repository.LoanRepositoryPort;
import org.lions.backend.domain.repository.RequesterRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanOrderService {

    private final LoanOrderRepositoryPort orderRepositoryPort;
    private final RequesterRepositoryPort requesterRepositoryPort;
    private final EquipmentRepositoryPort equipmentRepositoryPort;
    private final LoanRepositoryPort loanRepositoryPort;
    private final PdfTermService pdfTermService;

    @Transactional
    public LoanOrderResponse createOrder(LoanOrderCreateRequest request) {
        // Criação e validação do Solicitante / Beneficiário com regras LGPD
        Requester requester = Requester.builder()
                .isForSelf(request.isForSelf())
                .requesterName(request.getRequesterName())
                .relationship(request.getRelationship())
                .beneficiaryName(request.getBeneficiaryName())
                .documentType(request.getDocumentType())
                .documentNumber(request.getDocumentNumber())
                .whatsapp(request.getWhatsapp())
                .cep(request.getCep())
                .street(request.getStreet())
                .number(request.getNumber())
                .neighborhood(request.getNeighborhood())
                .city(request.getCity())
                .state(request.getState())
                .complement(request.getComplement())
                .needReason(request.getNeedReason())
                .consentGranted(Boolean.TRUE.equals(request.getConsentGranted()))
                .consentTimestamp(LocalDateTime.now())
                .assistedRegistration(request.isAssistedRegistration())
                .build();
        requester.validate();

        Requester savedRequester = requesterRepositoryPort.save(requester);

        // Criação do pedido de empréstimo
        LoanOrder order = LoanOrder.builder()
                .requester(savedRequester)
                .equipmentType(request.getEquipmentType())
                .quantity(request.getQuantity() != null && request.getQuantity() > 0 ? request.getQuantity() : 1)
                .status(OrderStatus.PENDENTE_ANALISE)
                .build();

        LoanOrder savedOrder = orderRepositoryPort.save(order);
        return toResponse(savedOrder);
    }

    public List<LoanOrderResponse> findAll(OrderStatus status) {
        List<LoanOrder> list = status != null
                ? orderRepositoryPort.findByStatus(status)
                : orderRepositoryPort.findAll();
        return list.stream().map(this::toResponse).toList();
    }

    public LoanOrderResponse findById(Long id) {
        LoanOrder order = orderRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de Empréstimo", id));
        return toResponse(order);
    }

    /**
     * RF04: Análise e aprovação pelo administrador (vincula o equipamento por patrimônio e gera o empréstimo)
     */
    @Transactional
    public LoanOrderResponse approveOrder(Long orderId, LoanApprovalRequest request) {
        LoanOrder order = orderRepositoryPort.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de Empréstimo", orderId));

        Equipment equipment = equipmentRepositoryPort.findById(request.getEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento", request.getEquipmentId()));

        if (!equipment.isAvailable()) {
            throw new BusinessException("O equipamento " + equipment.getPatrimonyNumber() + " não está disponível para empréstimo.");
        }

        order.approve();
        LoanOrder updatedOrder = orderRepositoryPort.save(order);

        // Cria o registro formal do empréstimo (RF05)
        Loan loan = Loan.builder()
                .order(updatedOrder)
                .equipment(equipment)
                .deliveryNotes(request.getDeliveryNotes())
                .deliveryPhotoUrl(request.getDeliveryPhotoUrl())
                .build();
        loan.initialize();

        // Salva equipamento com status EMPRESTADO
        equipmentRepositoryPort.save(equipment);

        // Gera o termo e hash SHA-256 preliminar
        PdfTermService.GeneratedTerm generatedTerm = pdfTermService.generateResponsibilityTerm(loan);
        loan.setTermHashSha256(generatedTerm.sha256Hash());

        loanRepositoryPort.save(loan);

        return toResponse(updatedOrder);
    }

    @Transactional
    public LoanOrderResponse rejectOrder(Long orderId, String rejectionReason) {
        LoanOrder order = orderRepositoryPort.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de Empréstimo", orderId));

        order.reject(rejectionReason);
        LoanOrder saved = orderRepositoryPort.save(order);
        return toResponse(saved);
    }

    @Transactional
    public LoanOrderResponse markPendingStock(Long orderId) {
        LoanOrder order = orderRepositoryPort.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de Empréstimo", orderId));

        order.markPendingStock();
        LoanOrder saved = orderRepositoryPort.save(order);
        return toResponse(saved);
    }

    public LoanOrderResponse toResponse(LoanOrder order) {
        Requester r = order.getRequester();
        String address = "";
        if (r.getStreet() != null) {
            address = String.format("%s, %s - %s, %s",
                    r.getStreet(),
                    r.getNumber() != null ? r.getNumber() : "S/N",
                    r.getNeighborhood() != null ? r.getNeighborhood() : "",
                    r.getCity() != null ? r.getCity() : "");
        }

        return LoanOrderResponse.builder()
                .id(order.getId())
                .isForSelf(r.isForSelf())
                .requesterName(r.getRequesterName())
                .relationship(r.getRelationship())
                .beneficiaryName(r.getBeneficiaryName())
                .documentType(r.getDocumentType())
                .maskedDocument(r.getMaskedDocument())
                .whatsapp(r.getWhatsapp())
                .addressSummary(address)
                .cep(r.getCep())
                .street(r.getStreet())
                .number(r.getNumber())
                .neighborhood(r.getNeighborhood())
                .city(r.getCity())
                .state(r.getState())
                .equipmentType(order.getEquipmentType())
                .equipmentTypeDescription(order.getEquipmentType() != null ? order.getEquipmentType().getDescription() : "")
                .quantity(order.getQuantity())
                .needReason(r.getNeedReason())
                .status(order.getStatus())
                .statusDescription(order.getStatus() != null ? order.getStatus().getDescription() : "")
                .rejectionReason(order.getRejectionReason())
                .assistedRegistration(r.isAssistedRegistration())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
