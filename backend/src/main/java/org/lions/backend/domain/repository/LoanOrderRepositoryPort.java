package org.lions.backend.domain.repository;

import org.lions.backend.domain.entity.LoanOrder;
import org.lions.backend.domain.enums.OrderStatus;

import java.util.List;
import java.util.Optional;

public interface LoanOrderRepositoryPort {
    LoanOrder save(LoanOrder order);
    Optional<LoanOrder> findById(Long id);
    List<LoanOrder> findAll();
    List<LoanOrder> findByStatus(OrderStatus status);
    long countByStatus(OrderStatus status);
}
