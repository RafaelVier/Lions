package org.lions.backend.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.request.PasswordResetRequest;
import org.lions.backend.application.dto.request.UserCreateRequest;
import org.lions.backend.application.dto.request.UserRoleRequest;
import org.lions.backend.application.dto.request.UserUpdateRequest;
import org.lions.backend.application.dto.response.UserResponse;
import org.lions.backend.application.service.user.UserService;
import org.lions.backend.domain.enums.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Usuários", description = "Gestão de usuários e perfis de acesso (RF01, RN01–RN03)")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Lista usuários, com filtro por perfil (ROLE_ADMIN, ROLE_OPERATOR, ROLE_VIEWER) e situação")
    public List<UserResponse> findAll(@RequestParam(name = "perfil", required = false) UserRole role,
                                      @RequestParam(name = "ativo", required = false) Boolean active) {
        return userService.findAll(role, active);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um usuário")
    public UserResponse findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um usuário (senha forte, usuário e e-mail únicos)")
    public UserResponse create(@Valid @RequestBody UserCreateRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza nome e e-mail")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return userService.update(id, request);
    }

    @PatchMapping("/{id}/perfil")
    @Operation(summary = "Altera o perfil (nunca o do próprio usuário)")
    public UserResponse changeRole(@PathVariable Long id, @Valid @RequestBody UserRoleRequest request) {
        return userService.changeRole(id, request);
    }

    @PatchMapping("/{id}/inativar")
    @Operation(summary = "Inativa um usuário (não a si mesmo; mantém ao menos um Admin ativo)")
    public UserResponse inactivate(@PathVariable Long id) {
        return userService.inactivate(id);
    }

    @PatchMapping("/{id}/ativar")
    @Operation(summary = "Reativa um usuário")
    public UserResponse activate(@PathVariable Long id) {
        return userService.activate(id);
    }

    @PatchMapping("/{id}/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Redefine a senha de outro usuário")
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetRequest request) {
        userService.resetPassword(id, request);
    }
}
