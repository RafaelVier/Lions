package org.lions.backend.infrastructure.persistence.repository;

import org.lions.backend.domain.enums.LoanStatus;
import org.lions.backend.infrastructure.persistence.entity.LoanJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface LoanJpaRepository extends JpaRepository<LoanJpaEntity, Long> {
    String GRAPH_BENEFICIARY = "beneficiary";
    String GRAPH_EQUIPMENT = "equipment";

    @EntityGraph(attributePaths = {GRAPH_BENEFICIARY, GRAPH_EQUIPMENT})
    List<LoanJpaEntity> findByStatus(LoanStatus status);

    @EntityGraph(attributePaths = {GRAPH_BENEFICIARY, GRAPH_EQUIPMENT})
    List<LoanJpaEntity> findByStatusIn(Collection<LoanStatus> statuses);

    @EntityGraph(attributePaths = {GRAPH_BENEFICIARY, GRAPH_EQUIPMENT})
    List<LoanJpaEntity> findByBeneficiaryIdOrderByLoanDateDesc(Long beneficiaryId);

    @EntityGraph(attributePaths = {GRAPH_BENEFICIARY, GRAPH_EQUIPMENT})
    List<LoanJpaEntity> findByEquipmentIdOrderByLoanDateDesc(Long equipmentId);

    @EntityGraph(attributePaths = {GRAPH_BENEFICIARY, GRAPH_EQUIPMENT})
    List<LoanJpaEntity> findByBeneficiaryIdAndStatusIn(Long beneficiaryId, Collection<LoanStatus> statuses);

    @EntityGraph(attributePaths = {GRAPH_BENEFICIARY, GRAPH_EQUIPMENT})
    @Query("SELECT l FROM LoanJpaEntity l WHERE l.status IN :statuses "
            + "AND l.expectedReturnDate BETWEEN :start AND :end ORDER BY l.expectedReturnDate")
    List<LoanJpaEntity> findByStatusInAndDueBetween(@Param("statuses") Collection<LoanStatus> statuses,
                                                    @Param("start") LocalDate start,
                                                    @Param("end") LocalDate end);

    @EntityGraph(attributePaths = {GRAPH_BENEFICIARY, GRAPH_EQUIPMENT})
    @Query("SELECT l FROM LoanJpaEntity l WHERE l.status IN :statuses "
            + "AND l.expectedReturnDate < :today ORDER BY l.expectedReturnDate")
    List<LoanJpaEntity> findByStatusInAndDueBefore(@Param("statuses") Collection<LoanStatus> statuses,
                                                   @Param("today") LocalDate today);

    @EntityGraph(attributePaths = {GRAPH_BENEFICIARY, GRAPH_EQUIPMENT})
    List<LoanJpaEntity> findByLoanDateBetweenOrderByLoanDate(LocalDate start, LocalDate end);

    long countByStatus(LoanStatus status);

    long countByLoanDateBetween(LocalDate start, LocalDate end);
}
