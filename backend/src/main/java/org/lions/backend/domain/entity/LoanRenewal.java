package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Registro imutável de uma renovação de empréstimo (RN23/RN25). */
@Getter
@Builder
@AllArgsConstructor
public class LoanRenewal {

    private final Long id;
    private final Long loanId;
    private final LocalDate previousDueDate;
    private final LocalDate newDueDate;
    private final String renewedBy;
    private final LocalDateTime renewedAt;
    private final LocalDateTime createdAt;
}
