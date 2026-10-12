package org.lions.backend.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lions.backend.domain.enums.MaintenanceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceResponse {
    private Long id;
    private Long equipmentId;
    private String patrimonyNumber;
    private String equipmentTypeDescription;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private BigDecimal cost;
    private MaintenanceStatus status;
    private String statusDescription;
    private LocalDateTime createdAt;
}
