package org.lions.backend.application.service.loan;

import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.response.BeneficiaryRestriction;
import org.lions.backend.domain.entity.Beneficiary;
import org.lions.backend.domain.entity.Loan;
import org.lions.backend.domain.enums.LoanStatus;
import org.lions.backend.domain.enums.OccurrenceStatus;
import org.lions.backend.domain.exception.ResourceNotFoundException;
import org.lions.backend.domain.repository.BeneficiaryRepositoryPort;
import org.lions.backend.domain.repository.LoanRepositoryPort;
import org.lions.backend.domain.repository.OccurrenceRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Contrato §6.2 — dono: Empréstimos (Frente D). */
@Service
@RequiredArgsConstructor
public class BeneficiaryRestrictionService {

    private final BeneficiaryRepositoryPort beneficiaryRepository;
    private final LoanRepositoryPort loanRepository;
    private final OccurrenceRepositoryPort occurrenceRepository;
    private final Clock clock;

    /**
     * Avalia se o beneficiário pode receber novo empréstimo/entrar na fila (RN20): bloqueia quando há empréstimo em
     * atraso, termo não assinado, ocorrência aberta, ou quando o cadastro está inativo/anonimizado.
     */
    @Transactional(readOnly = true)
    public BeneficiaryRestriction evaluate(Long beneficiaryId) {
        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiário", beneficiaryId));
        LocalDate today = LocalDate.now(clock);
        List<Loan> openLoans = loanRepository.findOpenByBeneficiaryId(beneficiaryId);
        boolean hasOpenOccurrence = occurrenceRepository.existsByBeneficiaryIdAndStatus(beneficiaryId,
                OccurrenceStatus.ABERTA);

        boolean inactive = beneficiary.isAnonymized() || !beneficiary.isActive();
        boolean overdue = openLoans.stream()
                .anyMatch(loan -> loan.getStatus() == LoanStatus.ATRASADO || loan.isOverdue(today));
        boolean pendingSignature = openLoans.stream()
                .anyMatch(loan -> loan.getStatus() == LoanStatus.PENDENTE_ASSINATURA);

        List<String> reasons = new ArrayList<>();
        if (beneficiary.isAnonymized()) {
            reasons.add("Beneficiário anonimizado.");
        } else if (!beneficiary.isActive()) {
            reasons.add("Beneficiário inativo.");
        }
        if (overdue) {
            reasons.add("Possui empréstimo em atraso.");
        }
        if (pendingSignature) {
            reasons.add("Possui termo de empréstimo pendente de assinatura.");
        }
        if (hasOpenOccurrence) {
            reasons.add("Possui ocorrência aberta (dano ou inadimplência).");
        }
        return new BeneficiaryRestriction(!reasons.isEmpty(), !openLoans.isEmpty(), hasOpenOccurrence, reasons,
                overdue, pendingSignature, inactive);
    }
}
