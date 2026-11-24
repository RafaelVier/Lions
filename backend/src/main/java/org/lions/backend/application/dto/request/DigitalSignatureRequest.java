package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Aceite eletrônico do termo (RN21). {@code signerName} vazio = quem deve assinar (beneficiário ou, se menor/incapaz,
 * o responsável legal — RN06). Se informado para beneficiário que exige responsável, deve ser o nome do responsável.
 */
public record DigitalSignatureRequest(
        @Size(max = 150, message = "O nome de quem assina deve ter no máximo 150 caracteres.")
        String signerName) {
}
