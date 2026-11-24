package org.lions.backend.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.lions.backend.domain.enums.EquipmentType;

/**
 * Solicitação de empréstimo (formulário público em 3 etapas ou cadastro assistido).
 * <ul>
 *   <li>Etapa 1 — quem solicita: {@code forSelf} (padrão {@code true}); para terceiros, {@code requesterName} e
 *   {@code requesterRelationship};</li>
 *   <li>Etapa 1/2 — beneficiário: {@code beneficiary} com os dados completos (RN05) e o consentimento (RN07).
 *   Se já existir beneficiário ativo com o documento, a solicitação é vinculada a ele (os dados não são alterados).
 *   Usuários autenticados podem informar {@code beneficiaryId} no lugar de {@code beneficiary};</li>
 *   <li>Etapa 2 — {@code equipmentType} e {@code needReason} (dado sensível de saúde — LGPD).</li>
 * </ul>
 */
public record LoanOrderCreateRequest(
        Boolean forSelf,
        @Size(max = 150, message = "O nome do solicitante deve ter no máximo 150 caracteres.")
        String requesterName,
        @Size(max = 60, message = "O parentesco/vínculo deve ter no máximo 60 caracteres.")
        String requesterRelationship,
        Long beneficiaryId,
        @Valid
        BeneficiaryRequest beneficiary,
        @NotNull(message = "O tipo de equipamento é obrigatório.")
        EquipmentType equipmentType,
        @Size(max = 2000, message = "O motivo deve ter no máximo 2000 caracteres.")
        String needReason) {
}
