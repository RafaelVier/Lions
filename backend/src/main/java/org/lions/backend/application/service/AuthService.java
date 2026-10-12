package org.lions.backend.application.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lions.backend.application.dto.request.LoginRequest;
import org.lions.backend.application.dto.request.RegisterRequest;
import org.lions.backend.application.dto.response.AuthResponse;
import org.lions.backend.domain.entity.User;
import org.lions.backend.domain.enums.UserRole;
import org.lions.backend.domain.exception.BusinessException;
import org.lions.backend.domain.repository.UserRepositoryPort;
import org.lions.backend.infrastructure.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @PostConstruct
    public void initDefaultAdmin() {
        if (!userRepositoryPort.existsByUsername("marcio")) {
            log.info("Inicializando usuário administrador padrão: marcio");
            User admin = User.builder()
                    .username("marcio")
                    .email("marcio@lionsmedianeira.org.br")
                    .password(passwordEncoder.encode("lions123"))
                    .role(UserRole.ROLE_ADMIN)
                    .active(true)
                    .build();
            userRepositoryPort.save(admin);
        }
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String token = jwtTokenProvider.generateToken(authentication);
        User user = userRepositoryPort.findByUsername(request.getUsername())
                .or(() -> userRepositoryPort.findByEmail(request.getUsername()))
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));

        return new AuthResponse(token, user.getUsername(), user.getEmail(), user.getRole());
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepositoryPort.existsByUsername(request.getUsername())) {
            throw new BusinessException("Nome de usuário já cadastrado.");
        }
        if (userRepositoryPort.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email já cadastrado.");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : UserRole.ROLE_MEMBER)
                .active(true)
                .build();

        User saved = userRepositoryPort.save(user);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        String token = jwtTokenProvider.generateToken(authentication);

        return new AuthResponse(token, saved.getUsername(), saved.getEmail(), saved.getRole());
    }
}
