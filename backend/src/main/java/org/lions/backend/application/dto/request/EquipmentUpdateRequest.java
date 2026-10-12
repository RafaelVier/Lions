package org.lions.backend.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.EquipmentType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentUpdateRequest {
    private String patrimonyNumber;
    private EquipmentType type;
    private EquipmentCondition condition;
    private EquipmentStatus status;
    private String notes;
    private String photoUrl;
}
