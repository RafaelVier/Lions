package org.lions.backend.domain.repository;

import org.lions.backend.domain.entity.Notification;
import org.lions.backend.domain.enums.NotificationStatus;
import org.lions.backend.domain.enums.NotificationType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NotificationRepositoryPort {
    Notification save(Notification notification);

    Optional<Notification> findById(Long id);

    List<Notification> findAll();

    /** Filtros opcionais ({@code null} = sem filtro), ordenado por {@code createdAt desc}. */
    List<Notification> findAll(NotificationStatus status, NotificationType type);

    List<Notification> findByStatus(NotificationStatus status);

    /** RN48 — status PENDENTE ou FALHA com {@code attempts < maxAttempts}. */
    List<Notification> findRetryable(int maxAttempts);

    /** RN46 — deduplicação por empréstimo, tipo e data de referência. */
    boolean existsByLoanIdAndTypeAndReferenceDate(Long loanId, NotificationType type, LocalDate referenceDate);
}
