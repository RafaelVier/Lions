package org.lions.backend.domain.repository;

import org.lions.backend.domain.entity.Beneficiary;

import java.util.List;
import java.util.Optional;

public interface BeneficiaryRepositoryPort {
    Beneficiary save(Beneficiary beneficiary);

    Optional<Beneficiary> findById(Long id);

    List<Beneficiary> findAll();

    /** {@code active == null} retorna todos. */
    List<Beneficiary> findAll(Boolean active);

    /** RN04 — beneficiário ativo com o documento (comparação exata; normalize antes). */
    Optional<Beneficiary> findActiveByCpf(String cpf);

    /** Busca por parte do nome (case-insensitive). */
    List<Beneficiary> searchByName(String name);

    /** Todos (ativos ou não) com o documento (comparação exata; normalize antes). */
    List<Beneficiary> findByCpf(String cpf);

    long count();

    long countByActive(boolean active);
}
