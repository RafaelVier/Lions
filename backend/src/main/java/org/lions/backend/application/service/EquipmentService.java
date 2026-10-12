package org.lions.backend.application.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lions.backend.application.dto.request.EquipmentCreateRequest;
import org.lions.backend.application.dto.request.EquipmentUpdateRequest;
import org.lions.backend.application.dto.response.EquipmentResponse;
import org.lions.backend.domain.entity.Equipment;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.exception.BusinessException;
import org.lions.backend.domain.exception.ResourceNotFoundException;
import org.lions.backend.domain.repository.EquipmentRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepositoryPort repositoryPort;

    @PostConstruct
    public void seedInitialEquipments() {
        if (repositoryPort.countTotal() == 0) {
            log.info("Inicializando acervo ortopédico com equipamentos exemplo...");
            // Exemplo baseado nos wireframes: PT-0142, PT-0087, PT-0053, etc.
            createSeedItem("PT-0142", EquipmentType.CADEIRA_DE_RODAS, EquipmentCondition.SEMINOVO, EquipmentStatus.DISPONIVEL, "Cadeira de rodas dobrável");
            createSeedItem("PT-0087", EquipmentType.ANDADOR, EquipmentCondition.BOM_ESTADO_OU_SEMINOVO(), EquipmentStatus.DISPONIVEL, "Andador de alumínio com 4 rodas");
            createSeedItem("PT-0053", EquipmentType.CADEIRA_DE_BANHO, EquipmentCondition.USADO, EquipmentStatus.DISPONIVEL, "Cadeira higiênica para banho");
            createSeedItem("PT-0019", EquipmentType.MULETAS, EquipmentCondition.NOVO, EquipmentStatus.DISPONIVEL, "Par de muletas canadenses ajustáveis");
            createSeedItem("PT-0020", EquipmentType.CADEIRA_DE_RODAS, EquipmentCondition.USADO, EquipmentStatus.DISPONIVEL, "Cadeira de rodas para adulto");
        }
    }

    private EquipmentCondition BOM_ESTADO_OU_SEMINOVO() {
        return EquipmentCondition.SEMINOVO;
    }

    private void createSeedItem(String pat, EquipmentType type, EquipmentCondition cond, EquipmentStatus status, String notes) {
        repositoryPort.save(Equipment.builder()
                .patrimonyNumber(pat)
                .type(type)
                .condition(cond)
                .status(status)
                .notes(notes)
                .build());
    }

    public EquipmentResponse create(EquipmentCreateRequest request) {
        if (repositoryPort.existsByPatrimonyNumber(request.getPatrimonyNumber())) {
            throw new BusinessException("Já existe um equipamento cadastrado com o patrimônio: " + request.getPatrimonyNumber());
        }

        Equipment equipment = Equipment.builder()
                .patrimonyNumber(request.getPatrimonyNumber())
                .type(request.getType())
                .condition(request.getCondition())
                .status(request.getStatus() != null ? request.getStatus() : EquipmentStatus.DISPONIVEL)
                .notes(request.getNotes())
                .photoUrl(request.getPhotoUrl())
                .build();
        equipment.validate();

        Equipment saved = repositoryPort.save(equipment);
        return toResponse(saved);
    }

    public EquipmentResponse findById(Long id) {
        Equipment equipment = repositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento", id));
        return toResponse(equipment);
    }

    public EquipmentResponse findByPatrimonyNumber(String patrimonyNumber) {
        Equipment equipment = repositoryPort.findByPatrimonyNumber(patrimonyNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento com patrimônio " + patrimonyNumber + " não encontrado"));
        return toResponse(equipment);
    }

    public List<EquipmentResponse> findAll(EquipmentStatus status, EquipmentType type) {
        List<Equipment> list;
        if (status != null && type != null) {
            list = repositoryPort.findByTypeAndStatus(type, status);
        } else if (status != null) {
            list = repositoryPort.findByStatus(status);
        } else {
            list = repositoryPort.findAll();
        }
        return list.stream().map(this::toResponse).toList();
    }

    public EquipmentResponse update(Long id, EquipmentUpdateRequest request) {
        Equipment equipment = repositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento", id));

        if (request.getPatrimonyNumber() != null && !request.getPatrimonyNumber().isBlank()) {
            if (!equipment.getPatrimonyNumber().equalsIgnoreCase(request.getPatrimonyNumber()) &&
                    repositoryPort.existsByPatrimonyNumber(request.getPatrimonyNumber())) {
                throw new BusinessException("O número de patrimônio " + request.getPatrimonyNumber() + " já está em uso.");
            }
            equipment.setPatrimonyNumber(request.getPatrimonyNumber());
        }
        if (request.getType() != null) equipment.setType(request.getType());
        if (request.getCondition() != null) equipment.setCondition(request.getCondition());
        if (request.getStatus() != null) equipment.setStatus(request.getStatus());
        if (request.getNotes() != null) equipment.setNotes(request.getNotes());
        if (request.getPhotoUrl() != null) equipment.setPhotoUrl(request.getPhotoUrl());

        Equipment saved = repositoryPort.save(equipment);
        return toResponse(saved);
    }

    public void delete(Long id) {
        Equipment equipment = repositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento", id));
        if (EquipmentStatus.EMPRESTADO.equals(equipment.getStatus())) {
            throw new BusinessException("Não é permitido excluir um equipamento que está atualmente emprestado.");
        }
        repositoryPort.deleteById(id);
    }

    public EquipmentResponse toResponse(Equipment e) {
        return EquipmentResponse.builder()
                .id(e.getId())
                .patrimonyNumber(e.getPatrimonyNumber())
                .type(e.getType())
                .typeDescription(e.getType() != null ? e.getType().getDescription() : "")
                .condition(e.getCondition())
                .conditionDescription(e.getCondition() != null ? e.getCondition().getDescription() : "")
                .status(e.getStatus())
                .statusDescription(e.getStatus() != null ? e.getStatus().getDescription() : "")
                .notes(e.getNotes())
                .photoUrl(e.getPhotoUrl())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
