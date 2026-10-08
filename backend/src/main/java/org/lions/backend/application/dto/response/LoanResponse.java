package org.lions.backend.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.LoanStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanResponse {
    private Long id;
    private Long orderId;
    private String beneficiaryName;
    private String requesterName;
    private String whatsapp;
    private Long equipmentId;
    private String patrimonyNumber;
    private String equipmentTypeDescription;
    private LocalDate loanDate;
    private LocalDate currentDueDate;
    private LoanStatus status;
    private String statusDescription;
    private EquipmentCondition initialCondition;
    private int renewalCount;
    private LocalDate lastRenewalDate;
    private String termHashSha256;
    private boolean termAccepted;
    private LocalDateTime electronicAcceptanceDate;
    private String acceptedByName;
    private String whatsappReminderUrl; // Link wa.me pronto para envio em um toque!
    private boolean isDueSoon;
    private boolean isOverdue;
}
