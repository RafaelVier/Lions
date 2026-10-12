package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.LoanStatus;
import org.lions.backend.domain.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Entidade de Domínio (DDD): Empréstimo de Equipamento
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Loan {

    private Long id;
    private LoanOrder order;
    private Equipment equipment;
    private LocalDate loanDate;
    private LocalDate initialExpectedReturnDate; // loanDate + 90 dias
    private LocalDate currentDueDate; // Atualizado a cada renovação
    private LoanStatus status;
    private EquipmentCondition initialCondition;
    private String deliveryPhotoUrl;
    private String deliveryNotes;
    private int renewalCount;
    private LocalDate lastRenewalDate;
    private String termPdfPath;
    private String termHashSha256;
    private LocalDateTime electronicAcceptanceDate;
    private String acceptedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void initialize() {
        if (loanDate == null) {
            loanDate = LocalDate.now();
        }
        if (initialExpectedReturnDate == null) {
            initialExpectedReturnDate = loanDate.plusDays(90);
        }
        if (currentDueDate == null) {
            currentDueDate = initialExpectedReturnDate;
        }
        if (status == null) {
            status = LoanStatus.ATIVO;
        }
        if (equipment != null) {
            this.initialCondition = equipment.getCondition();
            equipment.markAsLoaned();
        }
    }

    /**
     * RF09: Renovação sem novo cadastro e sem limite de renovações (+90 dias)
     */
    public void renew() {
        if (LoanStatus.DEVOLVIDO.equals(this.status)) {
            throw new BusinessException("Não é possível renovar um empréstimo já devolvido.");
        }
        this.renewalCount++;
        this.lastRenewalDate = LocalDate.now();
        // Adiciona 90 dias à data de vencimento atual ou a partir de hoje caso já esteja atrasado
        LocalDate baseDate = currentDueDate != null && currentDueDate.isAfter(LocalDate.now()) ? currentDueDate : LocalDate.now();
        this.currentDueDate = baseDate.plusDays(90);
        this.status = LoanStatus.RENOVADO;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsOverdue() {
        if (LoanStatus.ATIVO.equals(this.status) || LoanStatus.RENOVADO.equals(this.status)) {
            this.status = LoanStatus.ATRASADO;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public boolean isDueSoon() {
        if (currentDueDate == null || LoanStatus.DEVOLVIDO.equals(this.status)) {
            return false;
        }
        long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), currentDueDate);
        return daysUntilDue >= 0 && daysUntilDue <= 7;
    }

    public boolean isOverdue() {
        if (currentDueDate == null || LoanStatus.DEVOLVIDO.equals(this.status)) {
            return false;
        }
        return LocalDate.now().isAfter(currentDueDate);
    }
}
