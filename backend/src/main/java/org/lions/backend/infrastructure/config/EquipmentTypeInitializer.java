package org.lions.backend.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.lions.backend.application.service.equipment.EquipmentTypeService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Garante uma linha em {@code tb_equipment_types} para cada tipo do enum (a migration V1 já insere; em dev/teste o
 * schema é gerado pelo Hibernate e a tabela começa vazia). Roda antes do seed de dados de desenvolvimento.
 */
@Component
@Order(-10)
@RequiredArgsConstructor
public class EquipmentTypeInitializer implements ApplicationRunner {

    private final EquipmentTypeService equipmentTypeService;

    @Override
    public void run(ApplicationArguments args) {
        equipmentTypeService.ensureDefaults();
    }
}
