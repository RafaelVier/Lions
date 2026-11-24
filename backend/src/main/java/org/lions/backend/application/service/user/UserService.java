package org.lions.backend.application.service.user;

import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.request.PasswordResetRequest;
import org.lions.backend.application.dto.request.UserCreateRequest;
import org.lions.backend.application.dto.request.UserRoleRequest;
import org.lions.backend.application.dto.request.UserUpdateRequest;
import org.lions.backend.application.dto.response.UserResponse;
import org.lions.backend.application.port.CurrentUserPort;
import org.lions.backend.application.service.audit.AuditService;
import org.lions.backend.application.service.auth.SessionService;
import org.lions.backend.application.support.AuditDetails;
import org.lions.backend.application.support.PasswordPolicy;
import org.lions.backend.domain.entity.User;
import org.lions.backend.domain.enums.AuditAction;
import org.lions.backend.domain.enums.UserRole;
import org.lions.backend.domain.exception.BusinessException;
import org.lions.backend.domain.exception.ConflictException;
import org.lions.backend.domain.exception.ResourceNotFoundException;
import org.lions.backend.domain.repository.UserRepositoryPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Gestão de usuários pelo Admin (RF01): criação (RN02, RN57), edição auditada (RN58), troca de perfil (RN02 — nunca o
 * próprio), inativação sem exclusão física (RN03 — nunca a si; ao menos 1 Admin ativo) e redefinição de senha.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    public static final String ENTITY_NAME = "User";

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final SessionService sessionService;
    private final CurrentUserPort currentUser;

    @Transactional(readOnly = true)
    public List<UserResponse> findAll(UserRole role, Boolean active) {
        return userRepository.findAll().stream()
                .filter(u -> role == null || u.getRole() == role)
                .filter(u -> active == null || u.isActive() == active)
                .sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return UserResponse.from(find(id));
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Já existe usuário com este login.", Map.of("field", "username"));
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Já existe usuário com este e-mail.", Map.of("field", "email"));
        }
        PasswordPolicy.validate(request.password());
        User saved = userRepository.save(User.builder()
                .username(username)
                .fullName(blankToNull(request.fullName()))
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .active(true)
                .build());
        auditService.record(ENTITY_NAME, saved.getId(), AuditAction.CREATE, AuditDetails.of(
                "username", saved.getUsername(), "email", saved.getEmail(), "role", saved.getRole()));
        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = find(id);
        User before = find(id);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(email)) {
            throw new ConflictException("Já existe usuário com este e-mail.", Map.of("field", "email"));
        }
        user.setFullName(blankToNull(request.fullName()));
        user.setEmail(email);
        User saved = userRepository.save(user);
        var changes = auditService.diff(before, saved, "fullName", "email");
        if (!changes.isEmpty()) {
            auditService.record(ENTITY_NAME, id, AuditAction.UPDATE, changes);
        }
        return UserResponse.from(saved);
    }

    /** RN02 — o Admin não pode alterar o próprio perfil; deve restar ao menos um Admin ativo. */
    @Transactional
    public UserResponse changeRole(Long id, UserRoleRequest request) {
        User user = find(id);
        UserRole previous = user.getRole();
        if (previous == request.role()) {
            return UserResponse.from(user);
        }
        user.changeRole(request.role(), currentUser.username());
        if (previous == UserRole.ROLE_ADMIN && user.isActive() && activeAdmins() <= 1) {
            throw new BusinessException("Deve haver ao menos um Administrador ativo no sistema.");
        }
        User saved = userRepository.save(user);
        auditService.record(ENTITY_NAME, id, AuditAction.UPDATE, AuditDetails.of(
                "role", AuditDetails.oldNew(previous, saved.getRole())));
        return UserResponse.from(saved);
    }

    /** RN03 — inativação (bloqueia o login); nunca a si mesmo; mantém ao menos um Admin ativo. */
    @Transactional
    public UserResponse inactivate(Long id) {
        User user = find(id);
        if (user.getUsername().equalsIgnoreCase(currentUser.username())) {
            throw new BusinessException("Não é permitido inativar o próprio usuário.");
        }
        if (!user.isActive()) {
            throw new BusinessException("Usuário já está inativo.");
        }
        if (user.isAdmin() && activeAdmins() <= 1) {
            throw new BusinessException("Deve haver ao menos um Administrador ativo no sistema.");
        }
        user.deactivate();
        User saved = userRepository.save(user);
        int revoked = sessionService.revokeAll(saved.getUsername());
        auditService.record(ENTITY_NAME, id, AuditAction.INACTIVATE, AuditDetails.of(
                "active", AuditDetails.oldNew(true, false), "sessionsRevoked", revoked));
        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse activate(Long id) {
        User user = find(id);
        if (user.isActive()) {
            throw new BusinessException("Usuário já está ativo.");
        }
        user.activate();
        User saved = userRepository.save(user);
        auditService.record(ENTITY_NAME, id, AuditAction.REACTIVATE, AuditDetails.of(
                "active", AuditDetails.oldNew(false, true)));
        return UserResponse.from(saved);
    }

    @Transactional
    public void resetPassword(Long id, PasswordResetRequest request) {
        User user = find(id);
        PasswordPolicy.validate(request.newPassword());
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        auditService.record(ENTITY_NAME, id, AuditAction.UPDATE,
                AuditDetails.of("password", "redefinida pelo administrador"));
    }

    private long activeAdmins() {
        return userRepository.countByRoleAndActiveTrue(UserRole.ROLE_ADMIN);
    }

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
