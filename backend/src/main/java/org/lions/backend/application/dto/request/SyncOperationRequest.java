package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.lions.backend.domain.enums.ReturnCondition;
import org.lions.backend.domain.enums.SyncOperationType;

import java.time.LocalDateTime;

/**
 * Operação registrada offline (RN60). Campos por tipo:
 * <ul>
 *   <li>{@code RETIRADA}: {@code beneficiaryId}, {@code equipmentId} e {@code deliveryNotes?};</li>
 *   <li>{@code DEVOLUCAO}: {@code loanId}, {@code returnCondition} e {@code damageDescription?};</li>
 *   <li>{@code RENOVACAO}: {@code loanId}.</li>
 * </ul>
 *
 * @param clientOperationId id único gerado no dispositivo (UUID) — reenviar a mesma operação não a duplica
 * @param occurredAt        data/hora em que a operação foi feita no dispositivo
 */
public record SyncOperationRequest(
        @NotBlank(message = "O id da operação é obrigatório.")
        @Size(max = 64, message = "O id da operação deve ter no máximo 64 caracteres.")
        String clientOperationId,
        @NotNull(message = "O tipo da operação é obrigatório.")
        SyncOperationType type,
        @NotNull(message = "A data/hora da operação é obrigatória.")
        @PastOrPresent(message = "A data/hora da operação não pode ser futura.")
        LocalDateTime occurredAt,
        Long beneficiaryId,
        Long equipmentId,
        Long loanId,
        ReturnCondition returnCondition,
        @Size(max = 1000, message = "A descrição da avaria deve ter no máximo 1000 caracteres.")
        String damageDescription,
        @Size(max = 2000, message = "As observações de entrega devem ter no máximo 2000 caracteres.")
        String deliveryNotes) {
}
