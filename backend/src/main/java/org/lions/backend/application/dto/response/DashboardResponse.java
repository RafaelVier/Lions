package org.lions.backend.application.dto.response;

import org.lions.backend.domain.enums.EquipmentType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Painel de indicadores (RF15, UC39/UC40), calculado sobre a situação atual da base (tempo real).
 * <ul>
 *   <li>ocupação = emprestados / ativos (baixados não entram — RN54, RN55);</li>
 *   <li>itens em atraso e fila atual: situação de hoje;</li>
 *   <li>custos de manutenção e volume de empréstimos: no período {@code start..end} (padrão: últimos 30 dias).</li>
 * </ul>
 * Perfil Consulta recebe só números ({@code overdueItems} nulo, {@code personalDataHidden=true} — RN56).
 */
public record DashboardResponse(
        LocalDate start,
        LocalDate end,
        EquipmentType equipmentType,
        LocalDateTime generatedAt,
        boolean personalDataHidden,
        Occupancy occupancy,
        long overdueLoans,
        WaitlistSummary waitlist,
        BigDecimal maintenanceCost,
        LoanVolume loanVolume,
        List<TypeIndicator> byType,
        List<OverdueItem> overdueItems) {

    /** RN54 — {@code occupancyRate} em % (1 casa), sobre {@code activeEquipments} (não baixados). */
    public record Occupancy(long activeEquipments, long available, long reserved, long loaned, long inMaintenance,
                            long writtenOff, BigDecimal occupancyRate) {
    }

    public record WaitlistSummary(long waiting, long reserved) {
    }

    /** No período: empréstimos registrados (exceto cancelados), cancelados e devoluções; {@code openLoans} = hoje. */
    public record LoanVolume(long loans, long canceled, long returns, long openLoans) {
    }

    public record TypeIndicator(EquipmentType type, String description, long activeEquipments, long loaned,
                                BigDecimal occupancyRate, long overdueLoans, long waiting,
                                BigDecimal maintenanceCost, long loans) {
    }

    public record OverdueItem(Long loanId, String beneficiaryName, String patrimonyNumber, EquipmentType type,
                              LocalDate expectedReturnDate, long daysOverdue) {
    }
}
