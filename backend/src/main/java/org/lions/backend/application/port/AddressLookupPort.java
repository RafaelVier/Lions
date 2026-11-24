package org.lions.backend.application.port;

import org.lions.backend.application.dto.response.CepAddressResponse;

import java.util.Optional;

/** Consulta de endereço por CEP (ViaCEP). */
public interface AddressLookupPort {

    /**
     * @param cep 8 dígitos
     * @return endereço, ou vazio se o CEP não existir
     * @throws org.lions.backend.domain.exception.ExternalServiceException se o serviço estiver indisponível
     */
    Optional<CepAddressResponse> findByCep(String cep);
}
