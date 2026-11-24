package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lions.backend.domain.enums.SessionEndReason;

import java.time.LocalDateTime;

/** Sessão de um token JWT ({@code jti}) — RN57: o encerramento, inclusive por inatividade, fica registrado. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

    private Long id;
    private String jti;
    private String username;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime endedAt;
    private SessionEndReason endReason;

    public boolean isEnded() {
        return endedAt != null;
    }

    /** Encerra a sessão; sem efeito se já encerrada. Retorna {@code true} se encerrou agora. */
    public boolean end(SessionEndReason reason, LocalDateTime now) {
        if (isEnded()) {
            return false;
        }
        this.endedAt = now;
        this.endReason = reason;
        return true;
    }
}
