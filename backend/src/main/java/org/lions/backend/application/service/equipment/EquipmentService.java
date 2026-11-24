package org.lions.backend.application.service.equipment;

import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.request.EquipmentCreateRequest;
import org.lions.backend.application.dto.request.EquipmentSpecificationRequest;
import org.lions.backend.application.dto.request.EquipmentUpdateRequest;
import org.lions.backend.application.dto.request.WriteOffRequest;
import org.lions.backend.application.dto.response.EquipmentHistoryResponse;
import org.lions.backend.application.dto.response.EquipmentOperation;
import org.lions.backend.application.dto.response.EquipmentResponse;
import org.lions.backend.application.dto.response.EquipmentScanResponse;
import org.lions.backend.application.dto.response.LoanResponse;
import org.lions.backend.application.dto.response.MaintenanceResponse;
import org.lions.backend.application.event.EquipmentAvailableEvent;
import org.lions.backend.application.port.CurrentUserPort;
import org.lions.backend.application.port.QrCodePort;
import org.lions.backend.application.service.audit.AuditService;
import org.lions.backend.application.service.parameter.ParameterService;
import org.lions.backend.application.support.AuditDetails;
import org.lions.backend.domain.entity.Beneficiary;
import org.lions.backend.domain.entity.Equipment;
import org.lions.backend.domain.entity.EquipmentSpecification;
import org.lions.backend.domain.entity.Loan;
import org.lions.backend.domain.entity.Maintenance;
import org.lions.backend.domain.enums.AuditAction;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.LoanStatus;
import org.lions.backend.domain.enums.MaintenanceVerdict;
import org.lions.backend.domain.enums.UserRole;
import org.lions.backend.domain.exception.BusinessException;
import org.lions.backend.domain.exception.ResourceNotFoundException;
import org.lions.backend.domain.repository.BeneficiaryRepositoryPort;
import org.lions.backend.domain.repository.EquipmentRepositoryPort;
import org.lions.backend.domain.repository.LoanRepositoryPort;
import org.lions.backend.domain.repository.MaintenanceRepositoryPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Acervo (RF03/RF04): cadastro com patrimônio gerado (RN09), edição restrita (RN09/RN58), baixa (RN13), QR Code
 * (RN10), leitura com operações possíveis (RN15) e histórico (RN38). O status só muda pelos fluxos (RN12).
 */
@Service
@RequiredArgsConstructor
public class EquipmentService {

    public static final String ENTITY_NAME = "Equipment";
    public static final int QR_MIN_SIZE = 100;
    public static final int QR_MAX_SIZE = 1000;
    public static final int QR_DEFAULT_SIZE = 300;
    private static final String[] EDITABLE_FIELDS = {"type", "size", "manufactureYear", "specifications", "condition",
            "notes", "photoUrl"};

    private final EquipmentRepositoryPort equipmentRepository;
    private final LoanRepositoryPort loanRepository;
    private final MaintenanceRepositoryPort maintenanceRepository;
    private final BeneficiaryRepositoryPort beneficiaryRepository;
    private final ParameterService parameterService;
    private final AuditService auditService;
    private final CurrentUserPort currentUser;
    private final QrCodePort qrCodePort;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    /** RN09/RN14 — patrimônio gerado ({@code PT-0001}); status inicial DISPONIVEL; já entra na alocação da fila (RN32). */
    @Transactional
    public EquipmentResponse create(EquipmentCreateRequest request) {
        Equipment equipment = Equipment.builder()
                .type(request.getType())
                .size(blankToNull(request.getSize()))
                .manufactureYear(request.getManufactureYear())
                .specifications(toSpecifications(request.getSpecifications()))
                .condition(request.getCondition())
                .status(EquipmentStatus.DISPONIVEL)
                .notes(blankToNull(request.getNotes()))
                .photoUrl(blankToNull(request.getPhotoUrl()))
                .build();
        equipment.validate();
        Equipment saved = equipmentRepository.saveNew(equipment);
        auditService.record(ENTITY_NAME, saved.getId(), AuditAction.CREATE, AuditDetails.of(
                "patrimonyNumber", saved.getPatrimonyNumber(), "type", saved.getType(),
                "size", saved.getSize(), "condition", saved.getCondition()));
        events.publishEvent(new EquipmentAvailableEvent(saved.getId(), saved.getType()));
        return EquipmentResponse.from(find(saved.getId()));
    }

    @Transactional(readOnly = true)
    public List<EquipmentResponse> findAll(EquipmentStatus status, EquipmentType type, EquipmentCondition condition) {
        List<Equipment> equipments;
        if (status != null && type != null) {
            equipments = equipmentRepository.findByTypeAndStatus(type, status);
        } else if (status != null) {
            equipments = equipmentRepository.findByStatus(status);
        } else if (type != null) {
            equipments = equipmentRepository.findByType(type);
        } else {
            equipments = equipmentRepository.findAll();
        }
        return equipments.stream()
                .filter(e -> condition == null || e.getCondition() == condition)
                .sorted(Comparator.comparing(Equipment::getId))
                .map(EquipmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EquipmentResponse findById(Long id) {
        return EquipmentResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    public EquipmentResponse findByPatrimonyNumber(String patrimonyNumber) {
        return EquipmentResponse.from(findByCode(patrimonyNumber));
    }

    /** RN09 — patrimônio e status não são editáveis; alterações auditadas com valor anterior (RN58). */
    @Transactional
    public EquipmentResponse update(Long id, EquipmentUpdateRequest request) {
        Equipment equipment = find(id);
        Equipment before = find(id);
        if (!equipment.isActive()) {
            throw new BusinessException("Equipamento baixado não pode ser editado.");
        }
        if (request.getType() != null) {
            equipment.setType(request.getType());
        }
        if (request.getSize() != null) {
            equipment.setSize(blankToNull(request.getSize()));
        }
        if (request.getManufactureYear() != null) {
            equipment.setManufactureYear(request.getManufactureYear());
        }
        if (request.getSpecifications() != null) {
            equipment.setSpecifications(toSpecifications(request.getSpecifications()));
        }
        if (request.getCondition() != null) {
            equipment.setCondition(request.getCondition());
        }
        if (request.getNotes() != null) {
            equipment.setNotes(blankToNull(request.getNotes()));
        }
        if (request.getPhotoUrl() != null) {
            equipment.setPhotoUrl(blankToNull(request.getPhotoUrl()));
        }
        equipment.validate();
        Equipment saved = equipmentRepository.save(equipment);
        var changes = auditService.diff(before, saved, EDITABLE_FIELDS);
        if (!changes.isEmpty()) {
            auditService.record(ENTITY_NAME, id, AuditAction.UPDATE, changes);
        }
        return EquipmentResponse.from(saved);
    }

    /**
     * RN13 — baixa (exclusão lógica) somente por Admin, com motivo. Se houver manutenção em andamento, ela é
     * encerrada com parecer INSERVIVEL.
     */
    @Transactional
    public EquipmentResponse writeOff(Long id, WriteOffRequest request) {
        Equipment equipment = equipmentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento", id));
        EquipmentStatus previous = equipment.getStatus();
        LocalDate today = LocalDate.now(clock);
        String username = currentUser.username();
        if (previous == EquipmentStatus.EM_MANUTENCAO) {
            for (Maintenance maintenance : maintenanceRepository.findByEquipmentId(id)) {
                if (maintenance.isOpen()) {
                    maintenance.close(MaintenanceVerdict.INSERVIVEL,
                            "Encerrada pela baixa do equipamento (" + request.reason().getDescription() + ").",
                            username, today);
                    maintenanceRepository.save(maintenance);
                    auditService.record("Maintenance", maintenance.getId(), AuditAction.STATUS_CHANGE,
                            AuditDetails.of("status", maintenance.getStatus(), "reason", "Baixa do equipamento"));
                }
            }
        }
        equipment.writeOff(request.reason(), blankToNull(request.notes()), username, today);
        Equipment saved = equipmentRepository.save(equipment);
        auditService.record(ENTITY_NAME, id, AuditAction.INACTIVATE, AuditDetails.of(
                "status", AuditDetails.oldNew(previous, saved.getStatus()),
                "writeOffReason", request.reason(),
                "writeOffNotes", saved.getWriteOffNotes()));
        return EquipmentResponse.from(saved);
    }

    /** RN10 — QR Code com apenas o número de patrimônio (determinístico: reimpressão gera o mesmo código). */
    @Transactional(readOnly = true)
    public byte[] qrCode(Long id, Integer size) {
        Equipment equipment = find(id);
        int effective = size == null ? QR_DEFAULT_SIZE : size;
        if (effective < QR_MIN_SIZE || effective > QR_MAX_SIZE) {
            throw new BusinessException("O tamanho do QR Code deve estar entre " + QR_MIN_SIZE + " e "
                    + QR_MAX_SIZE + " pixels.");
        }
        return qrCodePort.generatePng(equipment.getPatrimonyNumber(), effective);
    }

    /** RN15 — leitura: localiza o item e lista as operações possíveis no status atual; nunca executa operação. */
    @Transactional(readOnly = true)
    public EquipmentScanResponse scan(String code) {
        Equipment equipment = findByCode(code);
        boolean admin = currentUser.hasRole(UserRole.ROLE_ADMIN);
        Optional<Loan> openLoan = loanRepository.findByEquipmentId(equipment.getId()).stream()
                .filter(Loan::isOpen)
                .findFirst();
        Optional<Maintenance> openMaintenance = maintenanceRepository.findByEquipmentId(equipment.getId()).stream()
                .filter(Maintenance::isOpen)
                .findFirst();

        List<EquipmentOperation> operations = new ArrayList<>();
        switch (equipment.getStatus()) {
            case DISPONIVEL -> {
                operations.add(EquipmentOperation.EMPRESTAR);
                operations.add(EquipmentOperation.ABRIR_MANUTENCAO);
                if (admin) {
                    operations.add(EquipmentOperation.BAIXAR);
                }
            }
            case RESERVADO -> operations.add(EquipmentOperation.EMPRESTAR);
            case EMPRESTADO -> openLoan.ifPresent(loan -> {
                if (loan.getStatus() == LoanStatus.PENDENTE_ASSINATURA) {
                    operations.add(EquipmentOperation.REGISTRAR_ASSINATURA);
                } else {
                    operations.add(EquipmentOperation.REGISTRAR_DEVOLUCAO);
                    operations.add(EquipmentOperation.RENOVAR);
                }
            });
            case EM_MANUTENCAO -> {
                operations.add(EquipmentOperation.LANCAR_CUSTO);
                operations.add(EquipmentOperation.ENCERRAR_MANUTENCAO);
                if (admin) {
                    operations.add(EquipmentOperation.BAIXAR);
                }
            }
            case BAIXADO -> {
                // somente consulta
            }
        }
        if (equipment.isActive()) {
            operations.add(EquipmentOperation.EDITAR);
            operations.add(EquipmentOperation.IMPRIMIR_QRCODE);
        }
        operations.add(EquipmentOperation.VER_HISTORICO);

        String reservedForName = equipment.getReservedForBeneficiaryId() == null ? null
                : beneficiaryRepository.findById(equipment.getReservedForBeneficiaryId())
                .map(Beneficiary::getFullName).orElse(null);
        return new EquipmentScanResponse(
                EquipmentResponse.from(equipment),
                operations,
                openLoan.map(Loan::getId).orElse(null),
                openLoan.map(loan -> loan.getBeneficiary().getId()).orElse(null),
                openLoan.map(loan -> loan.getBeneficiary().getFullName()).orElse(null),
                openLoan.map(Loan::getExpectedReturnDate).orElse(null),
                openMaintenance.map(Maintenance::getId).orElse(null),
                equipment.getReservedForBeneficiaryId(),
                reservedForName);
    }

    /** Histórico: empréstimos + manutenções + custo acumulado (RN38), inclusive de itens baixados (RN55). */
    @Transactional(readOnly = true)
    public EquipmentHistoryResponse history(Long id) {
        Equipment equipment = find(id);
        LocalDate today = LocalDate.now(clock);
        int dueSoonDays = parameterService.renewalWindowDays();
        List<LoanResponse> loans = loanRepository.findByEquipmentId(id).stream()
                .map(loan -> LoanResponse.from(loan, today, dueSoonDays))
                .toList();
        List<Maintenance> maintenances = maintenanceRepository.findByEquipmentId(id);
        BigDecimal totalCost = maintenances.stream()
                .map(Maintenance::totalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new EquipmentHistoryResponse(
                EquipmentResponse.from(equipment),
                loans,
                maintenances.stream().map(MaintenanceResponse::from).toList(),
                loans.size(),
                totalCost);
    }

    private Equipment find(Long id) {
        return equipmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Equipamento", id));
    }

    /** Aceita {@code PT-0001}, {@code pt-0001} ou apenas o número ({@code 1}). */
    private Equipment findByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("Informe o código (número de patrimônio) lido no QR Code.");
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        if (normalized.matches("\\d+")) {
            normalized = Equipment.formatPatrimony(Long.parseLong(normalized));
        }
        String patrimony = normalized;
        return equipmentRepository.findByPatrimonyNumber(patrimony)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento", patrimony));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static List<EquipmentSpecification> toSpecifications(List<EquipmentSpecificationRequest> requests) {
        List<EquipmentSpecification> result = new ArrayList<>();
        if (requests != null) {
            requests.stream()
                    .filter(Objects::nonNull)
                    .map(r -> new EquipmentSpecification(r.name() == null ? null : r.name().trim(),
                            r.value() == null ? null : r.value().trim()))
                    .forEach(result::add);
        }
        return result;
    }
}
