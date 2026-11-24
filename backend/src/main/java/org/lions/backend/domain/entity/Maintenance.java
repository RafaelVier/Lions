package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lions.backend.domain.enums.CostType;
import org.lions.backend.domain.enums.MaintenanceStatus;
import org.lions.backend.domain.enums.MaintenanceType;
import org.lions.backend.domain.enums.MaintenanceVerdict;
import org.lions.backend.domain.exception.BusinessException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Maintenance {

    private Long id;
    private Equipment equipment;
    private MaintenanceType type;
    @Builder.Default
    private MaintenanceStatus status = MaintenanceStatus.EM_ANDAMENTO;
    private LocalDate entryDate;
    private LocalDate exitDate;
    private String defectDescription;
    private String technicalResponsible;
    private String verdictNotes;
    private Long originLoanId;
    private String openedBy;
    private String closedBy;
    @Builder.Default
    private List<MaintenanceCost> costs = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** RN36 — abertura exige equipamento, tipo, descrição do defeito, data de entrada e responsável técnico. */
    public void validateOpening() {
        List<String> missing = new ArrayList<>();
        if (equipment == null) {
            missing.add("equipamento");
        }
        if (type == null) {
            missing.add("tipo de manutenção");
        }
        if (defectDescription == null || defectDescription.isBlank()) {
            missing.add("descrição do defeito");
        }
        if (entryDate == null) {
            missing.add("data de entrada");
        }
        if (technicalResponsible == null || technicalResponsible.isBlank()) {
            missing.add("responsável técnico");
        }
        if (!missing.isEmpty()) {
            throw new BusinessException("Campos obrigatórios da manutenção não informados: "
                    + String.join(", ", missing) + ".");
        }
        if (status == null) {
            status = MaintenanceStatus.EM_ANDAMENTO;
        }
        if (costs == null) {
            costs = new ArrayList<>();
        }
    }

    /** RN38 — lança custo (≥ 0) enquanto a manutenção está EM_ANDAMENTO. */
    public MaintenanceCost addCost(CostType costType, String description, String partReplaced, BigDecimal amount,
                                   String registeredBy, LocalDateTime now) {
        if (!isOpen()) {
            throw new BusinessException("Custos só podem ser lançados em manutenção em andamento; use um ajuste.");
        }
        validateCost(costType, description, amount);
        if (amount.signum() < 0) {
            throw new BusinessException("Valor do custo não pode ser negativo.");
        }
        return append(MaintenanceCost.builder()
                .maintenanceId(id)
                .type(costType)
                .description(description)
                .partReplaced(partReplaced)
                .amount(amount)
                .adjustment(false)
                .registeredBy(registeredBy)
                .registeredAt(now)
                .build());
    }

    /** Lançamento de ajuste (pode ser negativo), permitido em qualquer status, com motivo obrigatório (RN58). */
    public MaintenanceCost addAdjustment(CostType costType, String description, BigDecimal amount, String reason,
                                         String registeredBy, LocalDateTime now) {
        validateCost(costType, description, amount);
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Motivo do ajuste é obrigatório.");
        }
        return append(MaintenanceCost.builder()
                .maintenanceId(id)
                .type(costType)
                .description(description)
                .amount(amount)
                .adjustment(true)
                .adjustmentReason(reason)
                .registeredBy(registeredBy)
                .registeredAt(now)
                .build());
    }

    /**
     * RN37 — encerra com parecer. A transição do equipamento (APTO → DISPONIVEL; INSERVIVEL → baixa) é feita pelo
     * serviço com {@link Equipment#returnFromMaintenance} / {@link Equipment#writeOff}.
     */
    public void close(MaintenanceVerdict verdict, String notes, String by, LocalDate today) {
        if (!isOpen()) {
            throw new BusinessException("Manutenção já foi encerrada.");
        }
        if (verdict == null) {
            throw new BusinessException("Parecer da manutenção é obrigatório.");
        }
        if (entryDate != null && today.isBefore(entryDate)) {
            throw new BusinessException("Data de saída não pode ser anterior à data de entrada.");
        }
        this.status = verdict == MaintenanceVerdict.APTO
                ? MaintenanceStatus.CONCLUIDA_APTO
                : MaintenanceStatus.CONCLUIDA_INSERVIVEL;
        this.verdictNotes = notes;
        this.closedBy = by;
        this.exitDate = today;
    }

    public BigDecimal totalCost() {
        if (costs == null) {
            return BigDecimal.ZERO;
        }
        return costs.stream()
                .map(MaintenanceCost::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isOpen() {
        return status == MaintenanceStatus.EM_ANDAMENTO;
    }

    private MaintenanceCost append(MaintenanceCost cost) {
        if (costs == null) {
            costs = new ArrayList<>();
        }
        costs.add(cost);
        return cost;
    }

    private static void validateCost(CostType costType, String description, BigDecimal amount) {
        if (costType == null) {
            throw new BusinessException("Tipo do custo é obrigatório.");
        }
        if (description == null || description.isBlank()) {
            throw new BusinessException("Descrição do custo é obrigatória.");
        }
        if (amount == null) {
            throw new BusinessException("Valor do custo é obrigatório.");
        }
    }
}
