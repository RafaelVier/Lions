package org.lions.backend.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.lions.backend.domain.entity.Notification;
import org.lions.backend.domain.enums.NotificationStatus;
import org.lions.backend.domain.enums.NotificationType;
import org.lions.backend.domain.repository.NotificationRepositoryPort;
import org.lions.backend.infrastructure.persistence.entity.NotificationJpaEntity;
import org.lions.backend.infrastructure.persistence.repository.NotificationJpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final NotificationJpaRepository jpaRepository;

    @Override
    @Transactional
    public Notification save(Notification notification) {
        return toDomainEntity(jpaRepository.save(toJpaEntity(notification)));
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomainEntity);
    }

    @Override
    public List<Notification> findAll() {
        return map(jpaRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Override
    public List<Notification> findAll(NotificationStatus status, NotificationType type) {
        Specification<NotificationJpaEntity> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }
        return map(jpaRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Override
    public List<Notification> findByStatus(NotificationStatus status) {
        return map(jpaRepository.findByStatusOrderByCreatedAtDesc(status));
    }

    @Override
    public List<Notification> findRetryable(int maxAttempts) {
        return map(jpaRepository.findByStatusInAndAttemptsLessThanOrderByCreatedAtAsc(
                EnumSet.of(NotificationStatus.PENDENTE, NotificationStatus.FALHA), maxAttempts));
    }

    @Override
    public boolean existsByLoanIdAndTypeAndReferenceDate(Long loanId, NotificationType type,
                                                         LocalDate referenceDate) {
        return jpaRepository.existsByLoanIdAndTypeAndReferenceDate(loanId, type, referenceDate);
    }

    private List<Notification> map(List<NotificationJpaEntity> entities) {
        return entities.stream().map(this::toDomainEntity).toList();
    }

    public NotificationJpaEntity toJpaEntity(Notification domain) {
        if (domain == null) return null;
        return NotificationJpaEntity.builder()
                .id(domain.getId())
                .recipientType(domain.getRecipientType())
                .beneficiaryId(domain.getBeneficiaryId())
                .loanId(domain.getLoanId())
                .channel(domain.getChannel())
                .type(domain.getType())
                .message(domain.getMessage())
                .status(domain.getStatus())
                .attempts(domain.getAttempts())
                .lastError(domain.getLastError())
                .waLink(domain.getWaLink())
                .referenceDate(domain.getReferenceDate())
                .sentAt(domain.getSentAt())
                .resolvedBy(domain.getResolvedBy())
                .resolvedAt(domain.getResolvedAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public Notification toDomainEntity(NotificationJpaEntity entity) {
        if (entity == null) return null;
        return Notification.builder()
                .id(entity.getId())
                .recipientType(entity.getRecipientType())
                .beneficiaryId(entity.getBeneficiaryId())
                .loanId(entity.getLoanId())
                .channel(entity.getChannel())
                .type(entity.getType())
                .message(entity.getMessage())
                .status(entity.getStatus())
                .attempts(entity.getAttempts())
                .lastError(entity.getLastError())
                .waLink(entity.getWaLink())
                .referenceDate(entity.getReferenceDate())
                .sentAt(entity.getSentAt())
                .resolvedBy(entity.getResolvedBy())
                .resolvedAt(entity.getResolvedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
