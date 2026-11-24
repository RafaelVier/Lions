package org.lions.backend.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lions.backend.domain.entity.Equipment;
import org.lions.backend.domain.entity.EquipmentSpecification;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.WriteOffReason;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentResponse {
    private Long id;
    private String patrimonyNumber;
    private EquipmentType type;
    private String typeDescription;
    private String size;
    private Integer manufactureYear;
    private List<EquipmentSpecification> specifications;
    private EquipmentCondition condition;
    private String conditionDescription;
    private EquipmentStatus status;
    private String statusDescription;
    private String notes;
    private String photoUrl;
    private Long reservedForBeneficiaryId;
    private WriteOffReason writeOffReason;
    private String writeOffNotes;
    private LocalDate writeOffDate;
    private String writeOffBy;
    private LocalDate lastMaintenanceDate;
    private int loanCyclesSinceMaintenance;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EquipmentResponse from(Equipment e) {
        return EquipmentResponse.builder()
                .id(e.getId())
                .patrimonyNumber(e.getPatrimonyNumber())
                .type(e.getType())
                .typeDescription(e.getType() != null ? e.getType().getDescription() : null)
                .size(e.getSize())
                .manufactureYear(e.getManufactureYear())
                .specifications(e.getSpecifications() == null ? List.of() : List.copyOf(e.getSpecifications()))
                .condition(e.getCondition())
                .conditionDescription(e.getCondition() != null ? e.getCondition().getDescription() : null)
                .status(e.getStatus())
                .statusDescription(e.getStatus() != null ? e.getStatus().getDescription() : null)
                .notes(e.getNotes())
                .photoUrl(e.getPhotoUrl())
                .reservedForBeneficiaryId(e.getReservedForBeneficiaryId())
                .writeOffReason(e.getWriteOffReason())
                .writeOffNotes(e.getWriteOffNotes())
                .writeOffDate(e.getWriteOffDate())
                .writeOffBy(e.getWriteOffBy())
                .lastMaintenanceDate(e.getLastMaintenanceDate())
                .loanCyclesSinceMaintenance(e.getLoanCyclesSinceMaintenance())
                .version(e.getVersion())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
