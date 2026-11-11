package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lions.backend.domain.enums.HelpAudience;
import org.lions.backend.domain.enums.HelpContentType;
import org.lions.backend.domain.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/** Conteúdo de ajuda/FAQ/onboarding (RN53). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HelpContent {

    private Long id;
    private HelpContentType type;
    private String title;
    private String content;
    private String keywords;
    @Builder.Default
    private Set<HelpAudience> audiences = EnumSet.noneOf(HelpAudience.class);
    private int displayOrder;
    @Builder.Default
    private boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void validate() {
        if (type == null) {
            throw new BusinessException("Tipo do conteúdo é obrigatório.");
        }
        if (title == null || title.isBlank()) {
            throw new BusinessException("Título é obrigatório.");
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException("Conteúdo é obrigatório.");
        }
        if (audiences == null || audiences.isEmpty()) {
            throw new BusinessException("Informe ao menos um público para o conteúdo.");
        }
    }

    public void inactivate() {
        this.active = false;
    }

    public boolean isVisibleTo(HelpAudience audience) {
        return active && audiences != null && audiences.contains(audience);
    }
}
