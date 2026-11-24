package org.lions.backend.application.service.maintenance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lions.backend.application.dto.request.MaintenanceCloseRequest;
import org.lions.backend.application.dto.request.MaintenanceCostRequest;
import org.lions.backend.application.dto.request.MaintenanceCreateRequest;
import org.lions.backend.application.dto.request.MaintenanceUpdateRequest;
import org.lions.backend.application.dto.response.MaintenanceResponse;
import org.lions.backend.application.event.EquipmentAvailableEvent;
import org.lions.backend.application.event.EquipmentDamagedOnReturnEvent;
import org.lions.backend.application.port.CurrentUserPort;
import org.lions.backend.application.service.audit.AuditService;
import org.lions.backend.application.support.AuditDetails;
import org.lions.backend.domain.entity.Equipment;
import org.lions.backend.domain.entity.Maintenance;
import org.lions.backend.domain.entity.MaintenanceCost;
import org.lions.backend.domain.enums.AuditAction;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.MaintenanceStatus;
import org.lions.backend.domain.enums.MaintenanceType;
import org.lions.backend.domain.enums.MaintenanceVerdict;
import org.lions.backend.domain.enums.UserRole;
import org.lions.backend.domain.enums.WriteOffReason;
import org.lions.backend.domain.exception.BusinessException;
import org.lions.backend.domain.exception.ConflictException;
import org.lions.backend.domain.exception.ForbiddenOperationException;
import org.lions.backend.domain.exception.ResourceNotFoundException;
import org.lions.backend.domain.repository.EquipmentRepositoryPort;
import org.lions.backend.domain.repository.MaintenanceRepositoryPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Manutenções (RF08): abertura (RN36), custos e ajustes (RN38, RN41), encerramento com parecer (RN37) e abertura
 * automática de corretiva na devolução com avaria (RN24).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceService {

    public static final String ENTITY_NAME = "Maintenance";
    public static final String AUTO_TECHNICAL_RESPONSIBLE = "A definir";

    private final MaintenanceRepositoryPort maintenanceRepository;
    private final EquipmentRepositoryPort equipmentRepository;
    private final AuditService auditService;
    private final CurrentUserPort currentUser;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    /** RN36 — abre manutenção e coloca o item EM_MANUTENCAO (RN12: somente a partir de DISPONIVEL). */
    @Transactional
    public MaintenanceResponse open(MaintenanceCreateRequest request) {
        Equipment equipment = equipmentRepository.findByIdForUpdate(request.equipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento", request.equipmentId()));
        LocalDate today = LocalDate.now(clock);
        if (equipment.getStatus() == EquipmentStatus.EM_MANUTENCAO) {
            Long openId = maintenanceRepository.findByEquipmentId(equipment.getId()).stream()
                    .filter(Maintenance::isOpen).map(Maintenance::getId).findFirst().orElse(null);
            throw new ConflictException("Equipamento " + equipment.getPatrimonyNumber()
                    + " já possui manutenção em andamento.", AuditDetails.of("maintenanceId", openId));
        }
        if (equipment.getStatus() != EquipmentStatus.DISPONIVEL) {
            throw new BusinessException("Só é possível abrir manutenção de equipamento disponível (status atual: "
                    + equipment.getStatus().getDescription() + "). Registre a devolução ou libere a reserva antes.");
        }
        LocalDate entryDate = request.entryDate() != null ? request.entryDate() : today;
        if (entryDate.isAfter(today)) {
            throw new BusinessException("A data de entrada não pode ser futura.");
        }

        Maintenance maintenance = Maintenance.builder()
                .equipment(equipment)
                .type(request.type() != null ? request.type() : MaintenanceType.CORRETIVA)
                .status(MaintenanceStatus.EM_ANDAMENTO)
                .entryDate(entryDate)
                .defectDescription(request.defectDescription().trim())
                .technicalResponsible(request.technicalResponsible().trim())
                .openedBy(currentUser.username())
                .build();
        maintenance.validateOpening();

        equipment.sendToMaintenance();
        Equipment saved = equipmentRepository.save(equipment);
        maintenance.setEquipment(saved);
        Maintenance created = maintenanceRepository.save(maintenance);

        auditService.record(ENTITY_NAME, created.getId(), AuditAction.CREATE, AuditDetails.of(
                "equipmentId", saved.getId(), "type", created.getType(), "entryDate", created.getEntryDate(),
                "technicalResponsible", created.getTechnicalResponsible()));
        auditService.record("Equipment", saved.getId(), AuditAction.STATUS_CHANGE, AuditDetails.of(
                "status", AuditDetails.oldNew(EquipmentStatus.DISPONIVEL, saved.getStatus()),
                "maintenanceId", created.getId()));
        return MaintenanceResponse.from(created);
    }

    /** Ajusta defeito/responsável enquanto EM_ANDAMENTO (manutenção encerrada não é editada — RN41). */
    @Transactional
    public MaintenanceResponse update(Long id, MaintenanceUpdateRequest request) {
        Maintenance maintenance = find(id);
        Maintenance before = find(id);
        if (!maintenance.isOpen()) {
            throw new BusinessException("Manutenção encerrada não pode ser editada; use um lançamento de ajuste.");
        }
        if (request.defectDescription() != null && !request.defectDescription().isBlank()) {
            maintenance.setDefectDescription(request.defectDescription().trim());
        }
        if (request.technicalResponsible() != null && !request.technicalResponsible().isBlank()) {
            maintenance.setTechnicalResponsible(request.technicalResponsible().trim());
        }
        Maintenance saved = maintenanceRepository.save(maintenance);
        var changes = auditService.diff(before, saved, "defectDescription", "technicalResponsible");
        if (!changes.isEmpty()) {
            auditService.record(ENTITY_NAME, id, AuditAction.UPDATE, changes);
        }
        return MaintenanceResponse.from(saved);
    }

    /** RN38 (custo) / RN41 (ajuste — somente Admin, com motivo). */
    @Transactional
    public MaintenanceResponse addCost(Long id, MaintenanceCostRequest request) {
        Maintenance maintenance = find(id);
        LocalDateTime now = LocalDateTime.now(clock);
        String username = currentUser.username();
        MaintenanceCost cost;
        if (request.isAdjustment()) {
            if (!currentUser.hasRole(UserRole.ROLE_ADMIN)) {
                throw new ForbiddenOperationException("Lançamento de ajuste é permitido somente ao Administrador.");
            }
            cost = maintenance.addAdjustment(request.type(), request.description().trim(), request.amount(),
                    request.adjustmentReason(), username, now);
        } else {
            cost = maintenance.addCost(request.type(), request.description().trim(), blankToNull(request.partReplaced()),
                    request.amount(), username, now);
        }
        Maintenance saved = maintenanceRepository.save(maintenance);
        auditService.record(ENTITY_NAME, id, request.isAdjustment() ? AuditAction.ADJUSTMENT : AuditAction.UPDATE,
                AuditDetails.of("costType", cost.getType(), "description", cost.getDescription(),
                        "amount", cost.getAmount(), "adjustmentReason", cost.getAdjustmentReason(),
                        "totalCost", saved.totalCost()));
        return MaintenanceResponse.from(saved);
    }

    /** RN37 — APTO → DISPONIVEL (entra na alocação da fila); INSERVIVEL → baixa (somente Admin). */
    @Transactional
    public MaintenanceResponse close(Long id, MaintenanceCloseRequest request) {
        if (!currentUser.hasRole(UserRole.ROLE_ADMIN)) {
            // UC23 — o encerramento com parecer (APTO ou INSERVIVEL) é do Administrador
            throw new ForbiddenOperationException("Somente o Administrador pode encerrar manutenção (UC23).");
        }
        Maintenance maintenance = find(id);
        LocalDate today = LocalDate.now(clock);
        String username = currentUser.username();
        MaintenanceStatus previousStatus = maintenance.getStatus();
        maintenance.close(request.verdict(), blankToNull(request.notes()), username, today);

        Equipment equipment = equipmentRepository.findByIdForUpdate(maintenance.getEquipment().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento", maintenance.getEquipment().getId()));
        EquipmentStatus previousEquipmentStatus = equipment.getStatus();
        if (request.verdict() == MaintenanceVerdict.APTO) {
            equipment.returnFromMaintenance(today);
        } else {
            equipment.writeOff(WriteOffReason.INSERVIVEL, blankToNull(request.notes()), username, today);
        }
        Equipment savedEquipment = equipmentRepository.save(equipment);
        maintenance.setEquipment(savedEquipment);
        Maintenance saved = maintenanceRepository.save(maintenance);

        auditService.record(ENTITY_NAME, id, AuditAction.STATUS_CHANGE, AuditDetails.of(
                "status", AuditDetails.oldNew(previousStatus, saved.getStatus()),
                "verdict", request.verdict(), "verdictNotes", saved.getVerdictNotes(),
                "totalCost", saved.totalCost()));
        auditService.recordWithRule("Equipment", savedEquipment.getId(),
                request.verdict() == MaintenanceVerdict.APTO ? AuditAction.STATUS_CHANGE : AuditAction.INACTIVATE,
                "RN37", AuditDetails.of("status", AuditDetails.oldNew(previousEquipmentStatus,
                        savedEquipment.getStatus()), "maintenanceId", id));

        if (request.verdict() == MaintenanceVerdict.APTO) {
            events.publishEvent(new EquipmentAvailableEvent(savedEquipment.getId(), savedEquipment.getType()));
        }
        return MaintenanceResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    public List<MaintenanceResponse> findAll(MaintenanceStatus status, Long equipmentId, MaintenanceType type) {
        List<Maintenance> maintenances;
        if (equipmentId != null) {
            maintenances = maintenanceRepository.findByEquipmentId(equipmentId);
        } else if (status != null) {
            maintenances = maintenanceRepository.findByStatus(status);
        } else {
            maintenances = maintenanceRepository.findAll();
        }
        return maintenances.stream()
                .filter(m -> status == null || m.getStatus() == status)
                .filter(m -> type == null || m.getType() == type)
                .sorted(Comparator.comparing(Maintenance::getEntryDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Maintenance::getId, Comparator.reverseOrder()))
                .map(MaintenanceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MaintenanceResponse findById(Long id) {
        return MaintenanceResponse.from(find(id));
    }

    /** RN24 — devolução com avaria: abre manutenção corretiva automaticamente (item já está EM_MANUTENCAO). */
    @EventListener
    @Transactional
    public void onEquipmentDamagedOnReturn(EquipmentDamagedOnReturnEvent event) {
        Equipment equipment = equipmentRepository.findById(event.equipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento", event.equipmentId()));
        boolean alreadyOpen = maintenanceRepository.findByEquipmentId(equipment.getId()).stream()
                .anyMatch(Maintenance::isOpen);
        if (alreadyOpen) {
            return;
        }
        if (equipment.getStatus() != EquipmentStatus.EM_MANUTENCAO) {
            log.warn("Equipamento {} não está em manutenção após devolução com avaria (status {}).",
                    equipment.getPatrimonyNumber(), equipment.getStatus());
            return;
        }
        Maintenance maintenance = Maintenance.builder()
                .equipment(equipment)
                .type(MaintenanceType.CORRETIVA)
                .status(MaintenanceStatus.EM_ANDAMENTO)
                .entryDate(LocalDate.now(clock))
                .defectDescription(event.description())
                .technicalResponsible(AUTO_TECHNICAL_RESPONSIBLE)
                .originLoanId(event.loanId())
                .openedBy(CurrentUserPort.SYSTEM_USERNAME)
                .build();
        maintenance.validateOpening();
        Maintenance created = maintenanceRepository.save(maintenance);
        auditService.recordAutomatic(ENTITY_NAME, created.getId(), AuditAction.CREATE, "RN24", Map.of(
                "equipmentId", equipment.getId(), "loanId", event.loanId(), "type", MaintenanceType.CORRETIVA));
    }

    private Maintenance find(Long id) {
        return maintenanceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Manutenção", id));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
