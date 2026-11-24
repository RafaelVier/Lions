package org.lions.backend.application.dto.response;

import org.lions.backend.domain.entity.HelpContent;
import org.lions.backend.domain.enums.HelpAudience;
import org.lions.backend.domain.enums.HelpContentType;

import java.time.LocalDateTime;
import java.util.Set;

public record HelpContentResponse(
        Long id,
        HelpContentType type,
        String typeDescription,
        String title,
        String content,
        String keywords,
        Set<HelpAudience> audiences,
        int displayOrder,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static HelpContentResponse from(HelpContent h) {
        return new HelpContentResponse(h.getId(), h.getType(),
                h.getType() != null ? h.getType().getDescription() : null, h.getTitle(), h.getContent(),
                h.getKeywords(), h.getAudiences(), h.getDisplayOrder(), h.isActive(), h.getCreatedAt(),
                h.getUpdatedAt());
    }
}
