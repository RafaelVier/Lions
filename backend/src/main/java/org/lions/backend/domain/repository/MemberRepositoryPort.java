package org.lions.backend.domain.repository;

import org.lions.backend.domain.entity.Member;

import java.util.List;
import java.util.Optional;

public interface MemberRepositoryPort {
    Member save(Member member);
    Optional<Member> findById(Long id);
    List<Member> findAll();
    List<Member> findByClubId(Long clubId);
    boolean existsByEmail(String email);
    boolean existsByCpf(String cpf);
    boolean existsById(Long id);
    void deleteById(Long id);
}
