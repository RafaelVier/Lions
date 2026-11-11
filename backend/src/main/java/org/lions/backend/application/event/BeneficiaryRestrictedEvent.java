package org.lions.backend.application.event;

/** Beneficiário inativado/anonimizado/bloqueado — Fila remove as posições ativas (RN34). */
public record BeneficiaryRestrictedEvent(Long beneficiaryId, String reason) {
}
