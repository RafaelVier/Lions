package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lions.backend.domain.enums.MemberRole;
import org.lions.backend.domain.enums.Status;
import org.lions.backend.domain.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidade de Domínio Pura (DDD): Membro (Companheiro Leão)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Member {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String cpf;
    private MemberRole role;
    private LocalDate admissionDate;
    private Status status;
    private Long clubId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void validate() {
        if (name == null || name.isBlank()) {
            throw new BusinessException("O nome do membro é obrigatório.");
        }
        if (email == null || email.isBlank()) {
            throw new BusinessException("O email do membro é obrigatório.");
        }
        if (clubId == null) {
            throw new BusinessException("O membro deve estar vinculado a um Lions Clube.");
        }
    }

    public void suspend() {
        this.status = Status.SUSPENSO;
        this.updatedAt = LocalDateTime.now();
    }

    public void inactivate() {
        this.status = Status.INATIVO;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.status = Status.ATIVO;
        this.updatedAt = LocalDateTime.now();
    }
}
