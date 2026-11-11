package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.WriteOffReason;
import org.lions.backend.domain.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Equipment {

    public static final String PATRIMONY_FORMAT = "PT-%04d";

    private Long id;
    private String patrimonyNumber;
    private EquipmentType type;
    private String size;
    private EquipmentCondition condition;
    @Builder.Default
    private EquipmentStatus status = EquipmentStatus.DISPONIVEL;
    private String notes;
    private String photoUrl;
    private Long reservedForBeneficiaryId;
    private WriteOffReason writeOffReason;
    private String writeOffNotes;
    private LocalDate writeOffDate;
    private String writeOffBy;
    private LocalDate lastMaintenanceDate;
    private int loanCyclesSinceMaintenance;
    /** Controle de concorrência otimista (copiado de/para {@code @Version} da entidade JPA). */
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static String formatPatrimony(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Id é obrigatório para gerar o número de patrimônio.");
        }
        return String.format(PATRIMONY_FORMAT, id);
    }

    /** RN14 — tipo e conservação obrigatórios. */
    public void validate() {
        if (type == null) {
            throw new BusinessException("Tipo do equipamento é obrigatório.");
        }
        if (condition == null) {
            throw new BusinessException("Estado de conservação é obrigatório.");
        }
        if (status == null) {
            status = EquipmentStatus.DISPONIVEL;
        }
    }

    public void assignPatrimonyNumber() {
        this.patrimonyNumber = formatPatrimony(id);
    }

    /** RN32 — DISPONIVEL → RESERVADO para o beneficiário. */
    public void reserveFor(Long beneficiaryId) {
        if (beneficiaryId == null) {
            throw new BusinessException("Beneficiário da reserva é obrigatório.");
        }
        transitionTo(EquipmentStatus.RESERVADO);
        this.reservedForBeneficiaryId = beneficiaryId;
    }

    /** RESERVADO → DISPONIVEL. */
    public void releaseReservation() {
        if (status != EquipmentStatus.RESERVADO) {
            throw new BusinessException("Equipamento " + label() + " não está reservado.");
        }
        transitionTo(EquipmentStatus.DISPONIVEL);
        this.reservedForBeneficiaryId = null;
    }

    /** RN17 — empresta se DISPONIVEL, ou se RESERVADO para o mesmo beneficiário. */
    public void markAsLoaned(Long beneficiaryId) {
        if (status == EquipmentStatus.RESERVADO && !Objects.equals(reservedForBeneficiaryId, beneficiaryId)) {
            throw new BusinessException("Equipamento " + label() + " está reservado para outro beneficiário.");
        }
        transitionTo(EquipmentStatus.EMPRESTADO);
        this.reservedForBeneficiaryId = null;
    }

    /** RN24 — EMPRESTADO → DISPONIVEL ou EM_MANUTENCAO; conta um ciclo de empréstimo (RN39). */
    public void markAsReturned(boolean needsMaintenance) {
        if (status != EquipmentStatus.EMPRESTADO) {
            throw new BusinessException("Equipamento " + label() + " não está emprestado.");
        }
        transitionTo(needsMaintenance ? EquipmentStatus.EM_MANUTENCAO : EquipmentStatus.DISPONIVEL);
        this.loanCyclesSinceMaintenance++;
    }

    public void sendToMaintenance() {
        transitionTo(EquipmentStatus.EM_MANUTENCAO);
    }

    /** RN37 (APTO) — EM_MANUTENCAO → DISPONIVEL; zera os ciclos e registra a data da manutenção. */
    public void returnFromMaintenance(LocalDate maintenanceDate) {
        if (status != EquipmentStatus.EM_MANUTENCAO) {
            throw new BusinessException("Equipamento " + label() + " não está em manutenção.");
        }
        transitionTo(EquipmentStatus.DISPONIVEL);
        this.loanCyclesSinceMaintenance = 0;
        this.lastMaintenanceDate = maintenanceDate;
    }

    /** RN13 — baixa (exclusão lógica) com motivo; só a partir de DISPONIVEL ou EM_MANUTENCAO (RN12). */
    public void writeOff(WriteOffReason reason, String notes, String by, LocalDate today) {
        if (reason == null) {
            throw new BusinessException("Motivo da baixa é obrigatório.");
        }
        transitionTo(EquipmentStatus.BAIXADO);
        this.reservedForBeneficiaryId = null;
        this.writeOffReason = reason;
        this.writeOffNotes = notes;
        this.writeOffBy = by;
        this.writeOffDate = today;
    }

    public boolean isAvailable() {
        return status == EquipmentStatus.DISPONIVEL;
    }

    public boolean isActive() {
        return status != EquipmentStatus.BAIXADO;
    }

    private void transitionTo(EquipmentStatus target) {
        EquipmentStatus current = status == null ? EquipmentStatus.DISPONIVEL : status;
        if (!current.canTransitionTo(target)) {
            throw new BusinessException("Transição de status inválida para o equipamento " + label() + ": "
                    + current + " → " + target + ".");
        }
        this.status = target;
    }

    private String label() {
        return patrimonyNumber != null ? patrimonyNumber : String.valueOf(id);
    }
}
