package org.lions.backend.application.port;

import org.lions.backend.domain.enums.UserRole;

/** Usuário autenticado corrente (abstrai o Spring Security da camada de aplicação). */
public interface CurrentUserPort {

    String SYSTEM_USERNAME = "SISTEMA";

    /** Username autenticado ou {@value #SYSTEM_USERNAME} quando não há autenticação (rotinas agendadas, rotas públicas). */
    String username();

    boolean hasRole(UserRole role);

    boolean isAuthenticated();
}
