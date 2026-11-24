package org.lions.backend.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.request.EquipmentTypeUpdateRequest;
import org.lions.backend.application.dto.response.EquipmentTypeResponse;
import org.lions.backend.application.service.equipment.EquipmentTypeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tipos-equipamento")
@RequiredArgsConstructor
@Tag(name = "Tipos de equipamento", description = "Rótulo e prazo de empréstimo por tipo (RN14, RN18)")
@SecurityRequirement(name = "bearerAuth")
public class EquipmentTypeController {

    private final EquipmentTypeService equipmentTypeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    @Operation(summary = "Lista os tipos de equipamento com o prazo efetivo")
    public List<EquipmentTypeResponse> findAll() {
        return equipmentTypeService.findAll();
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualiza descrição e prazo do tipo (loanDays nulo = prazo padrão)")
    public EquipmentTypeResponse update(@PathVariable("codigo") String codigo,
                                        @Valid @RequestBody EquipmentTypeUpdateRequest request) {
        return equipmentTypeService.update(codigo, request);
    }
}
