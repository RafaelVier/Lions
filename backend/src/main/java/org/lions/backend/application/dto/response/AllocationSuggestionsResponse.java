package org.lions.backend.application.dto.response;

import org.lions.backend.domain.enums.EquipmentType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Sugestões do assistente (RN50/RN51 — somente Admin): cruza fila de espera e solicitações pendentes com os itens
 * disponíveis. É consultivo: nenhuma ação é executada.
 */
public record AllocationSuggestionsResponse(
        LocalDateTime generatedAt,
        String notice,
        List<Suggestion> suggestions,
        List<Attention> attention,
        List<TypeSummary> summary) {

    /** {@code source}: FILA (posição na fila) ou SOLICITACAO (pedido pendente de análise). */
    public record Suggestion(
            EquipmentType equipmentType,
            String equipmentTypeDescription,
            Long equipmentId,
            String patrimonyNumber,
            String source,
            Long waitlistEntryId,
            Long orderId,
            Long beneficiaryId,
            String beneficiaryName,
            boolean priority,
            Integer position,
            String reason) {
    }

    /** Pessoas da fila que não podem receber o item agora (bloqueio RN20) — exigem atenção do Admin. */
    public record Attention(
            Long waitlistEntryId,
            Long beneficiaryId,
            String beneficiaryName,
            EquipmentType equipmentType,
            List<String> reasons) {
    }

    public record TypeSummary(
            EquipmentType equipmentType,
            String equipmentTypeDescription,
            long available,
            long reserved,
            long waiting,
            long pendingOrders) {
    }
}
