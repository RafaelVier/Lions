package org.lions.backend.domain.repository;

import org.lions.backend.domain.entity.LionsClub;

import java.util.List;
import java.util.Optional;

public interface LionsClubRepositoryPort {
    LionsClub save(LionsClub club);
    Optional<LionsClub> findById(Long id);
    List<LionsClub> findAll();
    List<LionsClub> findByDistrict(String district);
    boolean existsById(Long id);
    void deleteById(Long id);
}
