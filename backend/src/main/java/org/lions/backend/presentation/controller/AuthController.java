package org.lions.backend.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.request.LoginRequest;
import org.lions.backend.application.dto.request.PasswordChangeRequest;
import org.lions.backend.application.dto.response.AuthResponse;
import org.lions.backend.application.dto.response.UserResponse;
import org.lions.backend.application.service.auth.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Login, renovação e encerramento de sessão, troca de senha (RF01, RN57)")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    @PostMapping("/login")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Autentica por usuário ou e-mail e devolve o token JWT (30 min)")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Emite um novo token e revoga o atual (renovação por atividade — RN57)")
    public AuthResponse refresh(@Parameter(hidden = true)
                                @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return authService.refresh(token(authorization));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Encerra a sessão revogando o token atual")
    public void logout(@Parameter(hidden = true) @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        authService.logout(token(authorization));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Dados do usuário autenticado")
    public UserResponse me() {
        return authService.me();
    }

    @PutMapping("/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Troca a própria senha (senha forte — RN57)")
    public void changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        authService.changePassword(request);
    }

    private static String token(String authorization) {
        return authorization != null && authorization.startsWith(BEARER_PREFIX)
                ? authorization.substring(BEARER_PREFIX.length())
                : null;
    }
}
