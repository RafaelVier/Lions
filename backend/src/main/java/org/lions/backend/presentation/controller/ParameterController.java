package org.lions.backend.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.request.ParameterUpdateRequest;
import org.lions.backend.application.dto.response.SystemParameterResponse;
import org.lions.backend.application.service.parameter.ParameterService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/parametros")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Parâmetros", description = "Prazos e limites configuráveis do sistema")
@SecurityRequirement(name = "bearerAuth")
public class ParameterController {

    private final ParameterService parameterService;

    @GetMapping
    @Operation(summary = "Lista os parâmetros com valor efetivo e default")
    public List<SystemParameterResponse> findAll() {
        return parameterService.findAll();
    }

    @PutMapping("/{chave}")
    @Operation(summary = "Atualiza um parâmetro (inteiro positivo)")
    public SystemParameterResponse update(@PathVariable("chave") String chave,
                                          @Valid @RequestBody ParameterUpdateRequest request) {
        return parameterService.update(chave, request.value());
    }
}
