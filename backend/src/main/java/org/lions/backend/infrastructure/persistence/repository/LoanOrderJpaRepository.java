package org.lions.backend.infrastructure.persistence.repository;

import org.lions.backend.domain.enums.OrderStatus;
import org.lions.backend.infrastructure.persistence.entity.LoanOrderJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanOrderJpaRepository extends JpaRepository<LoanOrderJpaEntity, Long> {
    @EntityGraph(attributePaths = "beneficiary")
    List<LoanOrderJpaEntity> findByStatus(OrderStatus status);

    @EntityGraph(attributePaths = "beneficiary")
    List<LoanOrderJpaEntity> findByBeneficiaryId(Long beneficiaryId);

    long countByStatus(OrderStatus status);
}
