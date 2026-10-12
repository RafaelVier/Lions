package org.lions.backend.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lions.backend.domain.enums.ReturnCondition;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanReturnResponse {
    private Long id;
    private Long loanId;
    private String patrimonyNumber;
    private String beneficiaryName;
    private LocalDate returnDate;
    private ReturnCondition returnCondition;
    private String returnConditionDescription;
    private String damageDescription;
    private String returnPhotoUrl;
    private boolean hasDamage;
    private boolean responsibilityAssigned;
    private LocalDateTime createdAt;
}
