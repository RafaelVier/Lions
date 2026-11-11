package org.lions.backend.domain.repository;

import org.lions.backend.domain.entity.WaitlistEntry;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.WaitlistStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Convenção: "ActiveByType" = posições AGUARDANDO do tipo (a fila propriamente dita);
 * "ActiveByBeneficiary" = posições AGUARDANDO ou RESERVADO (RN31).
 */
public interface WaitlistEntryRepositoryPort {
    WaitlistEntry save(WaitlistEntry entry);

    Optional<WaitlistEntry> findById(Long id);

    List<WaitlistEntry> findAll();

    /** AGUARDANDO do tipo, ordenado por {@code priority desc, queuedAt asc} (RN29/RN30). */
    List<WaitlistEntry> findActiveByType(EquipmentType type);

    List<WaitlistEntry> findByStatus(WaitlistStatus status);

    List<WaitlistEntry> findByBeneficiaryId(Long beneficiaryId);

    /** AGUARDANDO ou RESERVADO do beneficiário. */
    List<WaitlistEntry> findActiveByBeneficiaryId(Long beneficiaryId);

    /** RN31 — existe posição AGUARDANDO ou RESERVADO do beneficiário para o tipo. */
    boolean existsActiveByBeneficiaryIdAndType(Long beneficiaryId, EquipmentType type);

    /** RESERVADO com {@code reservationExpiresAt < now}. */
    List<WaitlistEntry> findExpiredReservations(LocalDateTime now);

    /** Quantidade de posições AGUARDANDO do tipo. */
    long countActiveByType(EquipmentType type);
}
