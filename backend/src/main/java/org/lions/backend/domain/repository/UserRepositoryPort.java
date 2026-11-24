package org.lions.backend.domain.repository;

import org.lions.backend.domain.entity.User;
import org.lions.backend.domain.enums.UserRole;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {
    User save(User user);

    Optional<User> findById(Long id);

    List<User> findAll();

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    long countByRoleAndActiveTrue(UserRole role);
}
