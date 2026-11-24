package org.lions.backend.infrastructure.client;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lions.backend.application.dto.response.CepAddressResponse;
import org.lions.backend.application.port.AddressLookupPort;
import org.lions.backend.domain.exception.ExternalServiceException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Adaptador ViaCEP (Feign). CEP inexistente → vazio; indisponibilidade → {@link ExternalServiceException}. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViaCepAddressLookupAdapter implements AddressLookupPort {

    private final ViaCepClient viaCepClient;

    @Override
    public Optional<CepAddressResponse> findByCep(String cep) {
        try {
            CepResponse response = viaCepClient.getAddressByCep(cep);
            if (response == null || Boolean.TRUE.equals(response.getErro()) || response.getCep() == null) {
                return Optional.empty();
            }
            return Optional.of(new CepAddressResponse(
                    response.getCep(),
                    emptyToNull(response.getLogradouro()),
                    emptyToNull(response.getComplemento()),
                    emptyToNull(response.getBairro()),
                    emptyToNull(response.getLocalidade()),
                    emptyToNull(response.getUf()),
                    emptyToNull(response.getIbge()),
                    emptyToNull(response.getDdd())));
        } catch (FeignException.BadRequest | FeignException.NotFound e) {
            return Optional.empty();
        } catch (FeignException e) {
            log.warn("Falha ao consultar o ViaCEP ({}): {}", e.status(), e.getMessage());
            throw new ExternalServiceException("Serviço de consulta de CEP indisponível no momento. "
                    + "Preencha o endereço manualmente.", e);
        } catch (RuntimeException e) {
            log.warn("Falha ao consultar o ViaCEP: {}", e.getMessage());
            throw new ExternalServiceException("Serviço de consulta de CEP indisponível no momento. "
                    + "Preencha o endereço manualmente.", e);
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
