package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.lions.backend.domain.enums.HelpAudience;
import org.lions.backend.domain.enums.HelpContentType;

import java.util.Set;

/** Conteúdo de ajuda (tutorial, dica, FAQ, onboarding) mantido pelo Admin e filtrado por público (RN53). */
public record HelpContentRequest(
        @NotNull(message = "O tipo do conteúdo é obrigatório.")
        HelpContentType type,
        @NotBlank(message = "O título é obrigatório.")
        @Size(max = 200, message = "O título deve ter no máximo 200 caracteres.")
        String title,
        @NotBlank(message = "O conteúdo é obrigatório.")
        String content,
        @Size(max = 500, message = "As palavras-chave devem ter no máximo 500 caracteres.")
        String keywords,
        @NotEmpty(message = "Informe ao menos um público.")
        Set<HelpAudience> audiences,
        Integer displayOrder) {
}
