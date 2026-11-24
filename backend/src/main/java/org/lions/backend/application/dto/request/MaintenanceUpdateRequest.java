package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.Size;

/** Correção de dados de manutenção EM ANDAMENTO (ex.: responsável técnico da corretiva automática). Nulos = mantém. */
public record MaintenanceUpdateRequest(
        @Size(max = 1000, message = "A descrição do defeito deve ter no máximo 1000 caracteres.")
        String defectDescription,
        @Size(max = 150, message = "O responsável técnico deve ter no máximo 150 caracteres.")
        String technicalResponsible) {
}
