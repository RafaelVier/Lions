package org.lions.backend.application.service;

import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.request.EquipmentCreateRequest;
import org.lions.backend.application.dto.request.EquipmentUpdateRequest;
import org.lions.backend.application.dto.response.EquipmentResponse;
import org.lions.backend.domain.entity.Equipment;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.exception.ResourceNotFoundException;
import org.lions.backend.domain.repository.EquipmentRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Cadastro básico de equipamentos (mínimo compilável da Onda 0).
 * Frente C reescreve com auditoria, eventos, baixa, QR Code e manutenção.
 */
@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepositoryPort repositoryPort;

    @Transactional
    public EquipmentResponse create(EquipmentCreateRequest request) {
        Equipment equipment = Equipment.builder()
                .type(request.getType())
                .size(request.getSize())
                .condition(request.getCondition())
                .status(EquipmentStatus.DISPONIVEL)
                .notes(request.getNotes())
                .photoUrl(request.getPhotoUrl())
                .build();
        equipment.validate();
        return EquipmentResponse.from(repositoryPort.saveNew(equipment));
    }

    @Transactional(readOnly = true)
    public EquipmentResponse findById(Long id) {
        return EquipmentResponse.from(repositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento", id)));
    }

    @Transactional(readOnly = true)
    public List<EquipmentResponse> findAll(EquipmentStatus status, EquipmentType type) {
        List<Equipment> list;
        if (status != null && type != null) {
            list = repositoryPort.findByTypeAndStatus(type, status);
        } else if (status != null) {
            list = repositoryPort.findByStatus(status);
        } else if (type != null) {
            list = repositoryPort.findByType(type);
        } else {
            list = repositoryPort.findAll();
        }
        return list.stream().map(EquipmentResponse::from).toList();
    }

    @Transactional
    public EquipmentResponse update(Long id, EquipmentUpdateRequest request) {
        Equipment equipment = repositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento", id));
        if (request.getType() != null) {
            equipment.setType(request.getType());
        }
        if (request.getSize() != null) {
            equipment.setSize(request.getSize());
        }
        if (request.getCondition() != null) {
            equipment.setCondition(request.getCondition());
        }
        if (request.getNotes() != null) {
            equipment.setNotes(request.getNotes());
        }
        if (request.getPhotoUrl() != null) {
            equipment.setPhotoUrl(request.getPhotoUrl());
        }
        equipment.validate();
        return EquipmentResponse.from(repositoryPort.save(equipment));
    }
}
