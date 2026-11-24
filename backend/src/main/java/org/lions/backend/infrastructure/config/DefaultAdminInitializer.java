package org.lions.backend.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.lions.backend.application.support.PasswordPolicy;
import org.lions.backend.domain.entity.User;
import org.lions.backend.domain.enums.UserRole;
import org.lions.backend.domain.repository.UserRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria o administrador inicial (plan §9) se ainda não existir usuário com o username configurado.
 * Propriedades: {@code lions.security.default-admin.username|password|email}.
 */
@Slf4j
@Component
@Order(0)
public class DefaultAdminInitializer implements ApplicationRunner {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String email;

    public DefaultAdminInitializer(
            UserRepositoryPort userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${lions.security.default-admin.username:marcio}") String username,
            @Value("${lions.security.default-admin.password:Lions@123}") String password,
            @Value("${lions.security.default-admin.email:marcio@lionsmedianeira.org.br}") String email) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
        this.email = email;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (username == null || username.isBlank() || userRepository.existsByUsername(username)) {
            return;
        }
        if (!PasswordPolicy.isStrong(password)) {
            log.warn("A senha do administrador inicial não atende à política de senha forte: {}",
                    PasswordPolicy.violations(password));
        }
        log.info("Criando administrador inicial: {}", username);
        userRepository.save(User.builder()
                .username(username)
                .fullName("Administrador")
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(UserRole.ROLE_ADMIN)
                .active(true)
                .build());
    }
}
