package org.lions.backend.application.service;

import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.response.LoanOrderResponse;
import org.lions.backend.domain.entity.LoanOrder;
import org.lions.backend.domain.enums.OrderStatus;
import org.lions.backend.domain.exception.ResourceNotFoundException;
import org.lions.backend.domain.repository.LoanOrderRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Consulta de solicitações (mínimo compilável da Onda 0).
 * Frente B reescreve com criação pública/assistida, aprovação, recusa e envio à fila.
 */
@Service
@RequiredArgsConstructor
public class LoanOrderService {

    private final LoanOrderRepositoryPort orderRepositoryPort;

    @Transactional(readOnly = true)
    public List<LoanOrderResponse> findAll(OrderStatus status) {
        List<LoanOrder> list = status != null
                ? orderRepositoryPort.findByStatus(status)
                : orderRepositoryPort.findAll();
        return list.stream().map(LoanOrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public LoanOrderResponse findById(Long id) {
        return LoanOrderResponse.from(orderRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação", id)));
    }
}
