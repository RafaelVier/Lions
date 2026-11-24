package org.lions.backend.domain.entity;

/** Especificação livre do equipamento (nome/valor — ex.: "Largura do assento" = "45 cm"). MER: tb_especificacao. */
public record EquipmentSpecification(String name, String value) {
}
