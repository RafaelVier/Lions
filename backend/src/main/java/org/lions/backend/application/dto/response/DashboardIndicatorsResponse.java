package org.lions.backend.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardIndicatorsResponse {

    // Contadores principais do topo do Wireframe
    private long totalEquipments;
    private long availableEquipments;
    private long loanedEquipments;
    private long maintenanceEquipments;

    // Pedidos pendentes de análise pelo Márcio
    private long pendingOrdersCount;
    private List<LoanOrderResponse> pendingOrders;

    // Empréstimos que vencem nesta semana (com botão 'Lembrar no WhatsApp')
    private List<LoanResponse> dueThisWeekLoans;

    // Empréstimos já atrasados (com botão 'Lembrar no WhatsApp')
    private List<LoanResponse> overdueLoans;
}
