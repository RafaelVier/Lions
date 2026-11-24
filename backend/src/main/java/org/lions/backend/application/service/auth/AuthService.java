package org.lions.backend.application.service.auth;

import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.request.LoginRequest;
import org.lions.backend.application.dto.request.PasswordChangeRequest;
import org.lions.backend.application.dto.response.AuthResponse;
import org.lions.backend.application.dto.response.UserResponse;
import org.lions.backend.application.port.CurrentUserPort;
import org.lions.backend.application.service.audit.AuditService;
import org.lions.backend.application.support.AuditDetails;
import org.lions.backend.application.support.PasswordPolicy;
import org.lions.backend.domain.entity.User;
import org.lions.backend.domain.enums.AuditAction;
import org.lions.backend.domain.enums.SessionEndReason;
import org.lions.backend.domain.exception.BusinessException;
import org.lions.backend.domain.exception.UnauthorizedException;
import org.lions.backend.domain.repository.UserRepositoryPort;
import org.lions.backend.infrastructure.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Sessão (RN57): login (auditado, registra último acesso e abre a sessão do token), refresh do token de 30 min,
 * logout encerrando a sessão do {@code jti} (auditado) e troca da própria senha. Não há auto-cadastro: usuários são criados pelo Admin.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String ENTITY_NAME = "User";

    private final UserRepositoryPort userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final SessionService sessionService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final CurrentUserPort currentUser;
    private final Clock clock;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername().trim(), request.getPassword()));
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("Usuário ou senha inválidos."));
        user.registerLogin(LocalDateTime.now(clock));
        User saved = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(authentication);
        startSession(token, saved.getUsername());
        auditService.recordAs(saved.getUsername(), ENTITY_NAME, saved.getId(), AuditAction.LOGIN, null);
        return session(token, saved);
    }

    /** Emite novo token para o usuário autenticado e encerra a sessão do anterior (renovação por atividade). */
    @Transactional
    public AuthResponse refresh(String currentToken) {
        User user = authenticatedUser();
        if (!user.isActive()) {
            throw new UnauthorizedException("Usuário inativo.");
        }
        String token = jwtTokenProvider.generateToken(user.getUsername(), List.of(user.getRole().name()));
        startSession(token, user.getUsername());
        endSession(currentToken, SessionEndReason.RENOVADA);
        return session(token, user);
    }

    /** Revoga o token atual e registra o encerramento da sessão na auditoria. */
    @Transactional
    public void logout(String currentToken) {
        endSession(currentToken, SessionEndReason.LOGOUT);
        userRepository.findByUsername(currentUser.username()).ifPresent(user ->
                auditService.record(ENTITY_NAME, user.getId(), AuditAction.LOGOUT, null));
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        return UserResponse.from(authenticatedUser());
    }

    /** Troca da própria senha: confere a atual e aplica a política de senha forte (RN57). */
    @Transactional
    public void changePassword(PasswordChangeRequest request) {
        User user = authenticatedUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException("Senha atual incorreta.");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new BusinessException("A nova senha deve ser diferente da atual.");
        }
        PasswordPolicy.validate(request.newPassword());
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        auditService.record(ENTITY_NAME, user.getId(), AuditAction.UPDATE,
                AuditDetails.of("password", "alterada pelo próprio usuário"));
    }

    private User authenticatedUser() {
        return userRepository.findByUsername(currentUser.username())
                .orElseThrow(() -> new UnauthorizedException("Sessão inválida. Faça login novamente."));
    }

    private void startSession(String token, String username) {
        sessionService.start(jwtTokenProvider.getJti(token), username, jwtTokenProvider.getExpiration(token));
    }

    private void endSession(String token, SessionEndReason reason) {
        if (token != null && jwtTokenProvider.validateToken(token)) {
            sessionService.end(jwtTokenProvider.getJti(token), reason);
        }
    }

    private AuthResponse session(String token, User user) {
        Instant expiresAt = jwtTokenProvider.getExpiration(token);
        return AuthResponse.bearer(token, jwtTokenProvider.getExpirationMs() / 1000, expiresAt,
                UserResponse.from(user));
    }
}
