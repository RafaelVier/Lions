package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.LoanStatus;
import org.lions.backend.domain.enums.SignatureType;
import org.lions.backend.domain.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Loan {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Long id;
    private Beneficiary beneficiary;
    private Equipment equipment;
    private LoanOrder order;
    @Builder.Default
    private LoanStatus status = LoanStatus.PENDENTE_ASSINATURA;
    private LocalDate loanDate;
    private LocalDate initialExpectedReturnDate;
    private LocalDate expectedReturnDate;
    private int renewalCount;
    private LocalDate lastRenewalDate;
    private boolean retroactive;
    private String retroactiveJustification;
    private EquipmentCondition initialCondition;
    private String deliveryNotes;
    private String deliveryPhotoUrl;
    private String termPdfPath;
    private String termHashSha256;
    private LocalDateTime termGeneratedAt;
    private SignatureType signatureType;
    private LocalDateTime signedAt;
    private String signedByName;
    private String signedDocumentPath;
    private String signatureRegisteredBy;
    private String createdBy;
    private LocalDateTime returnedAt;
    private String cancelReason;
    private String canceledBy;
    private LocalDateTime canceledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Validações de criação: RN22 (retirada não futura; retroativa exige justificativa) e prazo posterior à retirada.
     * Marca o empréstimo como retroativo quando {@code loanDate < today}.
     */
    public void validateNew(LocalDate today) {
        if (beneficiary == null) {
            throw new BusinessException("Beneficiário é obrigatório.");
        }
        if (equipment == null) {
            throw new BusinessException("Equipamento é obrigatório.");
        }
        if (loanDate == null) {
            throw new BusinessException("Data de retirada é obrigatória.");
        }
        if (loanDate.isAfter(today)) {
            throw new BusinessException("A data de retirada não pode ser futura.");
        }
        if (loanDate.isBefore(today)) {
            if (retroactiveJustification == null || retroactiveJustification.isBlank()) {
                throw new BusinessException("Empréstimo retroativo exige justificativa.");
            }
            this.retroactive = true;
        }
        if (expectedReturnDate == null || !expectedReturnDate.isAfter(loanDate)) {
            throw new BusinessException("A data prevista de devolução deve ser posterior à data de retirada.");
        }
        if (initialExpectedReturnDate == null) {
            this.initialExpectedReturnDate = expectedReturnDate;
        }
    }

    /** RN21 — PENDENTE_ASSINATURA → ATIVO com o registro da assinatura do termo. */
    public void registerSignature(SignatureType type, String signerName, String documentPath, String by,
                                  LocalDateTime now) {
        if (status != LoanStatus.PENDENTE_ASSINATURA) {
            throw new BusinessException("Só é possível registrar assinatura de empréstimo pendente de assinatura.");
        }
        if (type == null) {
            throw new BusinessException("Tipo de assinatura é obrigatório.");
        }
        if (signerName == null || signerName.isBlank()) {
            throw new BusinessException("Nome de quem assinou é obrigatório.");
        }
        if (type == SignatureType.FISICA_DIGITALIZADA && (documentPath == null || documentPath.isBlank())) {
            throw new BusinessException("Assinatura física exige o upload do termo digitalizado.");
        }
        this.signatureType = type;
        this.signedByName = signerName;
        this.signedDocumentPath = documentPath;
        this.signatureRegisteredBy = by;
        this.signedAt = now;
        this.status = LoanStatus.ATIVO;
    }

    /** RN23 — motivos que impedem a renovação (lista vazia = pode renovar). */
    public List<String> renewalBlockers(LocalDate today, RenewalPolicy policy, boolean waitlistForType) {
        List<String> reasons = new ArrayList<>();
        if (status == LoanStatus.ATRASADO || (status.isOpen() && isOverdue(today))) {
            reasons.add("Empréstimo em atraso não pode ser renovado.");
        } else if (status != LoanStatus.ATIVO) {
            reasons.add("Só empréstimos ativos podem ser renovados (status atual: " + status + ").");
        }
        if (renewalCount >= policy.maxRenewals()) {
            reasons.add("Limite de " + policy.maxRenewals() + " renovações atingido.");
        }
        if (waitlistForType) {
            reasons.add("Há fila de espera para este tipo de equipamento.");
        }
        if (expectedReturnDate != null) {
            LocalDate windowStart = expectedReturnDate.minusDays(policy.windowDays());
            if (today.isBefore(windowStart)) {
                reasons.add("Renovação permitida somente a partir de " + DATE.format(windowStart) + ".");
            }
        }
        return reasons;
    }

    public boolean canRenew(LocalDate today, RenewalPolicy policy, boolean waitlistForType) {
        return renewalBlockers(today, policy, waitlistForType).isEmpty();
    }

    /**
     * RN23 — renova o empréstimo para {@code newDueDate} (calculada pelo serviço: vencimento atual + prazo do tipo).
     * Retorna o registro imutável da renovação (ainda sem id).
     */
    public LoanRenewal renew(LocalDate newDueDate, LocalDate today, RenewalPolicy policy, boolean waitlistForType,
                             String renewedBy, LocalDateTime now) {
        List<String> blockers = renewalBlockers(today, policy, waitlistForType);
        if (!blockers.isEmpty()) {
            throw new BusinessException(String.join(" ", blockers));
        }
        if (newDueDate == null || !newDueDate.isAfter(expectedReturnDate)) {
            throw new BusinessException("A nova data de devolução deve ser posterior ao vencimento atual.");
        }
        LocalDate previous = expectedReturnDate;
        this.expectedReturnDate = newDueDate;
        this.renewalCount++;
        this.lastRenewalDate = today;
        return LoanRenewal.builder()
                .loanId(id)
                .previousDueDate(previous)
                .newDueDate(newDueDate)
                .renewedBy(renewedBy)
                .renewedAt(now)
                .build();
    }

    /** RN27 — ATIVO → ATRASADO a partir do dia seguinte ao vencimento. Retorna {@code true} se mudou. */
    public boolean markOverdue(LocalDate today) {
        if (status == LoanStatus.ATIVO && expectedReturnDate != null && today.isAfter(expectedReturnDate)) {
            this.status = LoanStatus.ATRASADO;
            return true;
        }
        return false;
    }

    public boolean isOverdue(LocalDate today) {
        return status != null && status.isOpen() && expectedReturnDate != null && today.isAfter(expectedReturnDate);
    }

    public long daysOverdue(LocalDate today) {
        return isOverdue(today) ? ChronoUnit.DAYS.between(expectedReturnDate, today) : 0;
    }

    /** Em aberto, não atrasado e vencendo em até {@code days} dias. */
    public boolean isDueSoon(LocalDate today, int days) {
        return status != null && status.isOpen() && expectedReturnDate != null && !isOverdue(today)
                && !expectedReturnDate.isAfter(today.plusDays(days));
    }

    public boolean isOpen() {
        return status != null && status.isOpen();
    }

    /** RN24/RN25 — ATIVO/ATRASADO → DEVOLVIDO. */
    public void markReturned(LocalDateTime now) {
        if (status != LoanStatus.ATIVO && status != LoanStatus.ATRASADO) {
            throw new BusinessException("Só empréstimos ativos ou atrasados podem ser devolvidos (status atual: "
                    + status + ").");
        }
        this.status = LoanStatus.DEVOLVIDO;
        this.returnedAt = now;
    }

    /** RN26 — cancelamento (nunca exclusão), com motivo. */
    public void cancel(String reason, String by, LocalDateTime now) {
        if (!isOpen()) {
            throw new BusinessException("Só empréstimos em aberto podem ser cancelados (status atual: " + status + ").");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Motivo do cancelamento é obrigatório.");
        }
        this.status = LoanStatus.CANCELADO;
        this.cancelReason = reason;
        this.canceledBy = by;
        this.canceledAt = now;
    }
}
