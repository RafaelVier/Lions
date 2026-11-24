package org.lions.backend.application.dto.response;

/** Endereço do CEP (ViaCEP) para autopreenchimento do formulário. */
public record CepAddressResponse(
        String cep,
        String street,
        String complement,
        String neighborhood,
        String city,
        String state,
        String ibgeCode,
        String ddd) {
}
