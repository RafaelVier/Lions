package org.lions.backend.infrastructure.persistence.repository;

import org.lions.backend.infrastructure.persistence.entity.BeneficiaryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BeneficiaryJpaRepository extends JpaRepository<BeneficiaryJpaEntity, Long> {
    List<BeneficiaryJpaEntity> findByActive(boolean active);

    Optional<BeneficiaryJpaEntity> findFirstByDocumentNumberAndActiveTrue(String documentNumber);

    @Query("SELECT b FROM BeneficiaryJpaEntity b WHERE LOWER(b.fullName) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY b.fullName")
    List<BeneficiaryJpaEntity> searchByName(@Param("name") String name);
}
