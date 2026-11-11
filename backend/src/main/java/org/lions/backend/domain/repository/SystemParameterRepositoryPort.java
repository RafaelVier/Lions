package org.lions.backend.domain.repository;

import org.lions.backend.domain.entity.SystemParameter;

import java.util.List;
import java.util.Optional;

public interface SystemParameterRepositoryPort {
    Optional<SystemParameter> findByKey(String key);

    List<SystemParameter> findAll();

    SystemParameter save(SystemParameter parameter);
}
