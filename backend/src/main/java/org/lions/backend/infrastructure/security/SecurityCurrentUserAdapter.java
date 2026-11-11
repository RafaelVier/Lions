package org.lions.backend.infrastructure.security;

import org.lions.backend.application.port.CurrentUserPort;
import org.lions.backend.domain.enums.UserRole;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityCurrentUserAdapter implements CurrentUserPort {

    @Override
    public String username() {
        Authentication authentication = currentAuthentication();
        return authentication != null ? authentication.getName() : SYSTEM_USERNAME;
    }

    @Override
    public boolean hasRole(UserRole role) {
        Authentication authentication = currentAuthentication();
        if (authentication == null || role == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role.name()::equals);
    }

    @Override
    public boolean isAuthenticated() {
        return currentAuthentication() != null;
    }

    private static Authentication currentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication;
    }
}
