package org.lions.backend.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.request.EquipmentCreateRequest;
import org.lions.backend.application.dto.request.EquipmentUpdateRequest;
import org.lions.backend.application.dto.request.WriteOffRequest;
import org.lions.backend.application.dto.response.EquipmentHistoryResponse;
import org.lions.backend.application.dto.response.EquipmentResponse;
import org.lions.backend.application.dto.response.EquipmentScanResponse;
import org.lions.backend.application.service.equipment.EquipmentService;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.EquipmentType;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/v1/equipamentos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
@Tag(name = "Equipamentos", description = "Cadastro, QR Code, leitura, baixa e histórico (RF03, RF04, RN09–RN15)")
@SecurityRequirement(name = "bearerAuth")
public class EquipmentController {

    private final EquipmentService equipmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra equipamento (número de patrimônio gerado automaticamente)")
    public EquipmentResponse create(@Valid @RequestBody EquipmentCreateRequest request) {
        return equipmentService.create(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Lista equipamentos com filtros por status, tipo e condição")
    public List<EquipmentResponse> findAll(@RequestParam(name = "status", required = false) EquipmentStatus status,
                                           @RequestParam(name = "tipo", required = false) EquipmentType type,
                                           @RequestParam(name = "condicao", required = false)
                                           EquipmentCondition condition) {
        return equipmentService.findAll(status, type, condition);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Detalha um equipamento")
    public EquipmentResponse findById(@PathVariable Long id) {
        return equipmentService.findById(id);
    }

    @GetMapping("/patrimonio/{numero}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Busca um equipamento pelo número de patrimônio")
    public EquipmentResponse findByPatrimonyNumber(@PathVariable("numero") String patrimonyNumber) {
        return equipmentService.findByPatrimonyNumber(patrimonyNumber);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza dados editáveis do equipamento (edição restrita conforme o status)")
    public EquipmentResponse update(@PathVariable Long id, @Valid @RequestBody EquipmentUpdateRequest request) {
        return equipmentService.update(id, request);
    }

    @PostMapping("/{id}/baixa")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Dá baixa no equipamento (inservível, perda, furto ou doação — RN13)")
    public EquipmentResponse writeOff(@PathVariable Long id, @Valid @RequestBody WriteOffRequest request) {
        return equipmentService.writeOff(id, request);
    }

    @GetMapping(value = "/{id}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "QR Code (PNG) com o número de patrimônio (RN10)")
    public ResponseEntity<byte[]> qrCode(@PathVariable Long id,
                                         @RequestParam(name = "tamanho", required = false) Integer size) {
        return FileResponses.inline(equipmentService.qrCode(id, size), MediaType.IMAGE_PNG_VALUE,
                "qrcode-equipamento-" + id + ".png");
    }

    @GetMapping("/leitura")
    @Operation(summary = "Leitura do QR Code: situação do item e operações disponíveis ao perfil (RN15)")
    public EquipmentScanResponse scan(@RequestParam("codigo") String code) {
        return equipmentService.scan(code);
    }

    @GetMapping("/{id}/historico")
    @Operation(summary = "Histórico do equipamento: empréstimos, manutenções e custo total")
    public EquipmentHistoryResponse history(@PathVariable Long id) {
        return equipmentService.history(id);
    }
}
