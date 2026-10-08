package org.lions.backend.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.EquipmentType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentResponse {
    private Long id;
    private String patrimonyNumber;
    private EquipmentType type;
    private String typeDescription;
    private EquipmentCondition condition;
    private String conditionDescription;
    private EquipmentStatus status;
    private String statusDescription;
    private String notes;
    private String photoUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
