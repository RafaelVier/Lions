package org.lions.backend.infrastructure.persistence.repository;

import org.lions.backend.infrastructure.persistence.entity.RequesterJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequesterJpaRepository extends JpaRepository<RequesterJpaEntity, Long> {
    Optional<RequesterJpaEntity> findByDocumentNumber(String documentNumber);

    @Query("SELECT r FROM RequesterJpaEntity r WHERE LOWER(r.beneficiaryName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(r.requesterName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<RequesterJpaEntity> searchByName(@Param("name") String name);
}
