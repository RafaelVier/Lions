package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lions.backend.domain.enums.Status;
import org.lions.backend.domain.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidade de Domínio Pura (DDD): Lions Clube
 * Raiz de Agregado do Domínio de Clubes
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LionsClub {

    private Long id;
    private String name;
    private String clubNumber;
    private String district; // Ex.: "LD-1", "LC-8"
    private String region;
    private String city;
    private String state;
    private String zipCode;
    private LocalDate foundationDate;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void validate() {
        if (name == null || name.isBlank()) {
            throw new BusinessException("O nome do Lions Clube é obrigatório.");
        }
        if (district == null || district.isBlank()) {
            throw new BusinessException("O distrito do Lions Clube é obrigatório.");
        }
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
