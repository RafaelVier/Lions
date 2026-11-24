package org.lions.backend.application.service.beneficiary;

import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.response.CepAddressResponse;
import org.lions.backend.application.port.AddressLookupPort;
import org.lions.backend.domain.exception.BusinessException;
import org.lions.backend.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

/** Consulta de CEP para autopreenchimento do endereço (rota pública). */
@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressLookupPort addressLookup;

    public CepAddressResponse findByCep(String cep) {
        String digits = cep == null ? "" : cep.replaceAll("\\D", "");
        if (digits.length() != 8) {
            throw new BusinessException("CEP inválido: informe 8 dígitos.");
        }
        return addressLookup.findByCep(digits)
                .orElseThrow(() -> new ResourceNotFoundException("CEP", cep));
    }
}
