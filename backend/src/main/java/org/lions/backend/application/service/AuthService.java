package org.lions.backend.application.service;

import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.request.LoginRequest;
import org.lions.backend.application.dto.response.AuthResponse;
import org.lions.backend.domain.entity.User;
import org.lions.backend.domain.exception.UnauthorizedException;
import org.lions.backend.domain.repository.UserRepositoryPort;
import org.lions.backend.infrastructure.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Autenticação (mínimo da Onda 0). Frente A acrescenta logout (revogação), gestão de usuários e troca de senha.
 * O admin inicial é criado por {@code DefaultAdminInitializer}; não há auto-cadastro.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepositoryPort userRepositoryPort;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final Clock clock;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userRepositoryPort.findByUsername(authentication.getName())
                .or(() -> userRepositoryPort.findByEmail(request.getUsername()))
                .orElseThrow(() -> new UnauthorizedException("Usuário ou senha inválidos."));
        user.registerLogin(LocalDateTime.now(clock));
        userRepositoryPort.save(user);

        String token = jwtTokenProvider.generateToken(authentication);
        return new AuthResponse(token, user.getUsername(), user.getEmail(), user.getRole());
    }
}
