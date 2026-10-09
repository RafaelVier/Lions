package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lions.backend.domain.enums.GlobalCause;
import org.lions.backend.domain.enums.Status;
import org.lions.backend.domain.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidade de Domínio Pura (DDD): Ação Social / Atividade de Serviço Comunitário
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialAction {

    private Long id;
    private String title;
    private String description;
    private GlobalCause cause;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer peopleImpacted;
    private Double volunteerHours;
    private Long clubId;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void validate() {
        if (title == null || title.isBlank()) {
            throw new BusinessException("O título da ação social é obrigatório.");
        }
        if (cause == null) {
            throw new BusinessException("A causa global do Lions é obrigatória.");
        }
        if (clubId == null) {
            throw new BusinessException("A ação deve estar vinculada a um Lions Clube organizador.");
        }
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException("A data de término não pode ser anterior à data de início.");
        }
    }
}
