package org.lions.backend.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.response.CepAddressResponse;
import org.lions.backend.application.service.beneficiary.AddressService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cep")
@RequiredArgsConstructor
@Tag(name = "CEP", description = "Consulta de endereço por CEP (público)")
public class CepController {

    private final AddressService addressService;

    @GetMapping("/{cep}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Busca o endereço do CEP (404 inexistente, 503 serviço indisponível)")
    public CepAddressResponse findByCep(@PathVariable String cep) {
        return addressService.findByCep(cep);
    }
}
