package org.lions.backend.infrastructure.persistence.repository;

import org.lions.backend.infrastructure.persistence.entity.LionsClubJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LionsClubJpaRepository extends JpaRepository<LionsClubJpaEntity, Long> {
    List<LionsClubJpaEntity> findByDistrict(String district);
}
