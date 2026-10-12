package org.lions.backend.domain.repository;

import org.lions.backend.domain.entity.Requester;

import java.util.List;
import java.util.Optional;

public interface RequesterRepositoryPort {
    Requester save(Requester requester);
    Optional<Requester> findById(Long id);
    Optional<Requester> findByDocumentNumber(String documentNumber);
    List<Requester> findAll();
    List<Requester> searchByName(String name);
}
