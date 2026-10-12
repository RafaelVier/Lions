package org.lions.backend.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.lions.backend.domain.entity.Member;
import org.lions.backend.domain.repository.MemberRepositoryPort;
import org.lions.backend.infrastructure.persistence.entity.MemberJpaEntity;
import org.lions.backend.infrastructure.persistence.repository.MemberJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MemberRepositoryAdapter implements MemberRepositoryPort {

    private final MemberJpaRepository jpaRepository;

    @Override
    public Member save(Member member) {
        MemberJpaEntity entity = toJpaEntity(member);
        MemberJpaEntity saved = jpaRepository.save(entity);
        return toDomainEntity(saved);
    }

    @Override
    public Optional<Member> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomainEntity);
    }

    @Override
    public List<Member> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomainEntity).toList();
    }

    @Override
    public List<Member> findByClubId(Long clubId) {
        return jpaRepository.findByClubId(clubId).stream().map(this::toDomainEntity).toList();
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByCpf(String cpf) {
        return jpaRepository.existsByCpf(cpf);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private MemberJpaEntity toJpaEntity(Member domain) {
        return MemberJpaEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .email(domain.getEmail())
                .phone(domain.getPhone())
                .cpf(domain.getCpf())
                .role(domain.getRole())
                .admissionDate(domain.getAdmissionDate())
                .status(domain.getStatus())
                .clubId(domain.getClubId())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private Member toDomainEntity(MemberJpaEntity entity) {
        return Member.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .cpf(entity.getCpf())
                .role(entity.getRole())
                .admissionDate(entity.getAdmissionDate())
                .status(entity.getStatus())
                .clubId(entity.getClubId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
