package org.lions.backend.application.dto.response;

import org.lions.backend.domain.enums.HelpContentType;

import java.util.List;

/** Respostas do assistente a partir da base de ajuda/FAQ (RN51 — nunca expõe dados pessoais). */
public record AssistantAnswerResponse(
        String question,
        List<Answer> answers,
        String message) {

    public record Answer(
            Long id,
            HelpContentType type,
            String title,
            String content,
            int score) {
    }
}
