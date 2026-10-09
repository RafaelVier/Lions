package org.lions.backend.infrastructure.persistence.repository;

import org.lions.backend.infrastructure.persistence.entity.MemberJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberJpaRepository extends JpaRepository<MemberJpaEntity, Long> {
    List<MemberJpaEntity> findByClubId(Long clubId);
    boolean existsByEmail(String email);
    boolean existsByCpf(String cpf);
}
