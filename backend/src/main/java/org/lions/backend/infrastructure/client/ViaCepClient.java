package org.lions.backend.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Spring Cloud OpenFeign client para consulta automática de CEP
 */
@FeignClient(name = "viaCepClient", url = "https://viacep.com.br/ws")
public interface ViaCepClient {

    @GetMapping("/{cep}/json")
    CepResponse getAddressByCep(@PathVariable("cep") String cep);
}
