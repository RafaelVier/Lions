package org.lions.backend.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.response.AllocationSuggestionsResponse;
import org.lions.backend.application.dto.response.AssistantAnswerResponse;
import org.lions.backend.application.service.assistant.AssistantService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assistente")
@RequiredArgsConstructor
@Tag(name = "Assistente", description = "Assistente por regras: sugestões de alocação e busca na FAQ (RF14, RN50, RN51)")
public class AssistantController {

    private final AssistantService assistantService;

    @GetMapping("/sugestoes-alocacao")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Sugere alocações de itens disponíveis para a fila (apenas sugestão — RN50)")
    public AllocationSuggestionsResponse allocationSuggestions() {
        return assistantService.allocationSuggestions();
    }

    @GetMapping("/perguntas")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Responde uma pergunta buscando na FAQ (RN51)")
    public AssistantAnswerResponse answer(@RequestParam(name = "q", required = false) String question) {
        return assistantService.answer(question);
    }
}
