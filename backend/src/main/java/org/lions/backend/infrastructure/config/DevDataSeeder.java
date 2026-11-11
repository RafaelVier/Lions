package org.lions.backend.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lions.backend.domain.entity.Equipment;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.repository.EquipmentRepositoryPort;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Dados de exemplo (somente perfil {@code dev}): 5 equipamentos quando o acervo está vazio. */
@Slf4j
@Component
@Profile("dev")
@Order(10)
@RequiredArgsConstructor
public class DevDataSeeder implements ApplicationRunner {

    private final EquipmentRepositoryPort equipmentRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (equipmentRepository.countTotal() > 0) {
            return;
        }
        log.info("Perfil dev: cadastrando equipamentos de exemplo.");
        seed(EquipmentType.CADEIRA_DE_RODAS, "Adulto", EquipmentCondition.BOM, "Cadeira de rodas dobrável");
        seed(EquipmentType.ANDADOR, null, EquipmentCondition.BOM, "Andador de alumínio articulado");
        seed(EquipmentType.CADEIRA_DE_BANHO, null, EquipmentCondition.REGULAR, "Cadeira higiênica para banho");
        seed(EquipmentType.MULETAS, "Regulável", EquipmentCondition.BOM, "Par de muletas canadenses");
        seed(EquipmentType.OUTROS, null, EquipmentCondition.REGULAR, "Cama hospitalar manual 2 manivelas");
    }

    private void seed(EquipmentType type, String size, EquipmentCondition condition, String notes) {
        Equipment equipment = Equipment.builder()
                .type(type)
                .size(size)
                .condition(condition)
                .status(EquipmentStatus.DISPONIVEL)
                .notes(notes)
                .build();
        equipment.validate();
        equipmentRepository.saveNew(equipment);
    }
}
