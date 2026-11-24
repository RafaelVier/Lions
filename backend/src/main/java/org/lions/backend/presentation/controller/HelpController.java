package org.lions.backend.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.request.HelpContentRequest;
import org.lions.backend.application.dto.response.HelpContentResponse;
import org.lions.backend.application.service.help.HelpContentService;
import org.lions.backend.domain.enums.HelpContentType;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ajuda")
@RequiredArgsConstructor
@Tag(name = "Ajuda", description = "Conteúdo de ajuda, tutoriais e FAQ (RF13, RN53)")
public class HelpController {

    private final HelpContentService helpContentService;

    @GetMapping("/publico")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Conteúdo de ajuda público (sem login)")
    public List<HelpContentResponse> findPublic(@RequestParam(name = "tipo", required = false) HelpContentType type) {
        return helpContentService.findPublic(type);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Conteúdo de ajuda visível ao perfil do usuário (todos=true lista também inativos — só Admin)")
    public List<HelpContentResponse> findForCurrentUser(
            @RequestParam(name = "tipo", required = false) HelpContentType type,
            @RequestParam(name = "todos", defaultValue = "false") boolean all) {
        return helpContentService.findForCurrentUser(type, all);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Detalha um conteúdo de ajuda (para edição)")
    public HelpContentResponse findById(@PathVariable Long id) {
        return helpContentService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cria conteúdo de ajuda")
    public HelpContentResponse create(@Valid @RequestBody HelpContentRequest request) {
        return helpContentService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Atualiza conteúdo de ajuda")
    public HelpContentResponse update(@PathVariable Long id, @Valid @RequestBody HelpContentRequest request) {
        return helpContentService.update(id, request);
    }

    @PatchMapping("/{id}/inativar")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Inativa conteúdo de ajuda")
    public HelpContentResponse inactivate(@PathVariable Long id) {
        return helpContentService.setActive(id, false);
    }

    @PatchMapping("/{id}/ativar")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Reativa conteúdo de ajuda")
    public HelpContentResponse activate(@PathVariable Long id) {
        return helpContentService.setActive(id, true);
    }
}
