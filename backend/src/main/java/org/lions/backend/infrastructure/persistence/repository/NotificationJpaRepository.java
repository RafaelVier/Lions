package org.lions.backend.infrastructure.persistence.repository;

import org.lions.backend.domain.enums.NotificationStatus;
import org.lions.backend.domain.enums.NotificationType;
import org.lions.backend.infrastructure.persistence.entity.NotificationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, Long>,
        JpaSpecificationExecutor<NotificationJpaEntity> {
    List<NotificationJpaEntity> findByStatusOrderByCreatedAtDesc(NotificationStatus status);

    List<NotificationJpaEntity> findByStatusInAndAttemptsLessThanOrderByCreatedAtAsc(
            Collection<NotificationStatus> statuses, int maxAttempts);

    boolean existsByLoanIdAndTypeAndReferenceDate(Long loanId, NotificationType type, LocalDate referenceDate);
}
