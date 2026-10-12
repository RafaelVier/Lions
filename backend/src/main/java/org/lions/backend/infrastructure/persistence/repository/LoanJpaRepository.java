package org.lions.backend.infrastructure.persistence.repository;

import org.lions.backend.domain.enums.LoanStatus;
import org.lions.backend.infrastructure.persistence.entity.LoanJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoanJpaRepository extends JpaRepository<LoanJpaEntity, Long> {
    List<LoanJpaEntity> findByStatus(LoanStatus status);

    @Query("SELECT l FROM LoanJpaEntity l WHERE l.currentDueDate BETWEEN :startDate AND :endDate AND l.status IN ('ATIVO', 'RENOVADO')")
    List<LoanJpaEntity> findDueBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT l FROM LoanJpaEntity l WHERE l.currentDueDate < :cutoffDate AND l.status IN ('ATIVO', 'RENOVADO', 'ATRASADO')")
    List<LoanJpaEntity> findOverdue(@Param("cutoffDate") LocalDate cutoffDate);

    List<LoanJpaEntity> findByEquipmentId(Long equipmentId);

    @Query("SELECT l FROM LoanJpaEntity l WHERE l.order.requester.id = :requesterId")
    List<LoanJpaEntity> findByRequesterId(@Param("requesterId") Long requesterId);

    long countByStatus(LoanStatus status);
}
