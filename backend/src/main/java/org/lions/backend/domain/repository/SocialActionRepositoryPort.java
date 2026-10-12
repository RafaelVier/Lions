package org.lions.backend.domain.repository;

import org.lions.backend.domain.entity.SocialAction;
import org.lions.backend.domain.enums.GlobalCause;

import java.util.List;
import java.util.Optional;

public interface SocialActionRepositoryPort {
    SocialAction save(SocialAction action);
    Optional<SocialAction> findById(Long id);
    List<SocialAction> findAll();
    List<SocialAction> findByClubId(Long clubId);
    List<SocialAction> findByCause(GlobalCause cause);
    boolean existsById(Long id);
    void deleteById(Long id);
}
