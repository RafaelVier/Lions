package org.lions.backend.application.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lions.backend.application.dto.response.NotificationResponse;
import org.lions.backend.application.port.CurrentUserPort;
import org.lions.backend.application.port.NotificationSenderPort;
import org.lions.backend.application.service.audit.AuditService;
import org.lions.backend.application.service.parameter.ParameterService;
import org.lions.backend.application.support.WhatsAppMessageHelper;
import org.lions.backend.domain.entity.Beneficiary;
import org.lions.backend.domain.entity.Notification;
import org.lions.backend.domain.enums.AuditAction;
import org.lions.backend.domain.enums.NotificationChannel;
import org.lions.backend.domain.enums.NotificationStatus;
import org.lions.backend.domain.enums.NotificationType;
import org.lions.backend.domain.enums.RecipientType;
import org.lions.backend.domain.exception.ResourceNotFoundException;
import org.lions.backend.domain.repository.BeneficiaryRepositoryPort;
import org.lions.backend.domain.repository.NotificationRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Notificações (RN46–RN49). Beneficiário: uma notificação por canal autorizado (RN47); sem canal → tarefa de contato
 * manual; falhas são retentadas até {@code notification.max-attempts} e depois viram contato manual (RN48).
 * Administradores: notificação interna (in-app), já marcada como enviada.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final String ENTITY_NAME = "Notification";

    private final NotificationRepositoryPort notificationRepository;
    private final BeneficiaryRepositoryPort beneficiaryRepository;
    private final NotificationSenderPort sender;
    private final ParameterService parameterService;
    private final WhatsAppMessageHelper whatsApp;
    private final AuditService auditService;
    private final CurrentUserPort currentUser;
    private final Clock clock;

    // ---------- contrato §6.2 ----------

    /** Registra/envia notificação ao beneficiário pelos canais consentidos (RN07, RN46–RN48). */
    @Transactional
    public void notifyBeneficiary(Long beneficiaryId, Long loanId, NotificationType type, String message) {
        notifyBeneficiary(beneficiaryId, loanId, type, message, null);
    }

    /** Notifica os administradores (RN39, RN49). */
    @Transactional
    public void notifyAdmins(NotificationType type, String message) {
        notifyAdmins(type, message, null, null);
    }

    // ---------- acréscimos ----------

    /**
     * Como {@link #notifyBeneficiary(Long, Long, NotificationType, String)}, com data de referência para
     * deduplicação (RN46: não repete o mesmo aviso do mesmo empréstimo no mesmo dia). Retorna as notificações criadas.
     */
    @Transactional
    public List<Notification> notifyBeneficiary(Long beneficiaryId, Long loanId, NotificationType type,
                                                String message, LocalDate referenceDate) {
        if (loanId != null && referenceDate != null
                && notificationRepository.existsByLoanIdAndTypeAndReferenceDate(loanId, type, referenceDate)) {
            return List.of();
        }
        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiário", beneficiaryId));
        if (beneficiary.isAnonymized()) {
            log.debug("Notificação {} ignorada: beneficiário {} anonimizado.", type, beneficiaryId);
            return List.of();
        }
        String waLink = waLink(beneficiary.getPhone(), message);
        Set<NotificationChannel> channels = EnumSet.noneOf(NotificationChannel.class);
        if (beneficiary.getNotificationChannels() != null) {
            channels.addAll(beneficiary.getNotificationChannels());
        }

        if (channels.isEmpty()) {
            // RN47 — sem canal autorizado: tarefa de contato manual para o Admin
            Notification manual = base(beneficiary.getId(), loanId, type, message, referenceDate, waLink, null);
            manual.requireManualContact("Beneficiário sem canal de notificação autorizado.");
            return List.of(notificationRepository.save(manual));
        }

        int maxAttempts = parameterService.notificationMaxAttempts();
        LocalDateTime now = LocalDateTime.now(clock);
        List<Notification> created = new ArrayList<>();
        for (NotificationChannel channel : channels) {
            Notification notification = base(beneficiary.getId(), loanId, type, message, referenceDate,
                    channel == NotificationChannel.EMAIL ? null : waLink, channel);
            attemptDelivery(notification, beneficiary, maxAttempts, now);
            created.add(notificationRepository.save(notification));
        }
        return created;
    }

    /**
     * Notificação interna aos administradores. Com {@code referenceKey}+{@code referenceDate} não duplica
     * (ex.: preventiva uma vez por item/mês). Retorna {@code true} se criou.
     */
    @Transactional
    public boolean notifyAdmins(NotificationType type, String message, String referenceKey, LocalDate referenceDate) {
        if (referenceKey != null && referenceDate != null
                && notificationRepository.existsByTypeAndReferenceKeyAndReferenceDate(type, referenceKey,
                referenceDate)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        notificationRepository.save(Notification.builder()
                .recipientType(RecipientType.ADMINISTRADOR)
                .type(type)
                .message(message)
                .status(NotificationStatus.ENVIADA)
                .sentAt(now)
                .referenceKey(referenceKey)
                .referenceDate(referenceDate)
                .build());
        return true;
    }

    /** RN48 — retenta notificações pendentes/falhas abaixo do limite de tentativas. Retorna quantas foram processadas. */
    @Transactional
    public int retryFailed() {
        int maxAttempts = parameterService.notificationMaxAttempts();
        LocalDateTime now = LocalDateTime.now(clock);
        int processed = 0;
        for (Notification notification : notificationRepository.findRetryable(maxAttempts)) {
            if (notification.getRecipientType() != RecipientType.BENEFICIARIO || notification.getChannel() == null) {
                continue;
            }
            Optional<Beneficiary> beneficiary = beneficiaryRepository.findById(notification.getBeneficiaryId());
            if (beneficiary.isEmpty() || beneficiary.get().isAnonymized()) {
                notification.requireManualContact("Beneficiário indisponível para envio.");
            } else {
                attemptDelivery(notification, beneficiary.get(), maxAttempts, now);
            }
            notificationRepository.save(notification);
            processed++;
        }
        return processed;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> findAll(NotificationStatus status, NotificationType type,
                                              RecipientType recipientType, Long beneficiaryId) {
        List<Notification> notifications = beneficiaryId != null
                ? notificationRepository.findByBeneficiaryId(beneficiaryId)
                : notificationRepository.findAll(status, type);
        return toResponses(notifications.stream()
                .filter(n -> status == null || n.getStatus() == status)
                .filter(n -> type == null || n.getType() == type)
                .filter(n -> recipientType == null || n.getRecipientType() == recipientType)
                .toList());
    }

    /** RN47/RN48 — tarefas de contato manual pendentes (com link wa.me pronto). */
    @Transactional(readOnly = true)
    public List<NotificationResponse> findManualContacts() {
        return toResponses(notificationRepository.findByStatus(NotificationStatus.AGUARDANDO_CONTATO_MANUAL));
    }

    /** Conclui manualmente (contato feito pelo WhatsApp/telefone). */
    @Transactional
    public NotificationResponse complete(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação", id));
        NotificationStatus previous = notification.getStatus();
        notification.completeManually(currentUser.username(), LocalDateTime.now(clock));
        Notification saved = notificationRepository.save(notification);
        auditService.record(ENTITY_NAME, id, AuditAction.STATUS_CHANGE,
                Map.of("status", Map.of(AuditService.OLD, previous, AuditService.NEW, saved.getStatus())));
        return toResponses(List.of(saved)).get(0);
    }

    /** Link wa.me com a mensagem pronta (ou {@code null} sem telefone). */
    public String waLink(String phone, String message) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        try {
            return whatsApp.buildLink(phone, message);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ---------- internos ----------

    private void attemptDelivery(Notification notification, Beneficiary beneficiary, int maxAttempts,
                                 LocalDateTime now) {
        NotificationChannel channel = notification.getChannel();
        String destination = channel == NotificationChannel.EMAIL ? beneficiary.getEmail() : beneficiary.getPhone();
        if (destination == null || destination.isBlank()) {
            notification.requireManualContact("Destino não cadastrado para o canal " + channel.getDescription() + ".");
            return;
        }
        try {
            NotificationSenderPort.DeliveryResult result = sender.send(channel, destination, notification.getMessage());
            if (result == NotificationSenderPort.DeliveryResult.SENT) {
                notification.markSent(now);
            } else {
                notification.requireManualContact("Canal " + channel.getDescription()
                        + " sem envio automático configurado: enviar pelo link.");
            }
        } catch (RuntimeException e) {
            log.warn("Falha ao enviar notificação {} pelo canal {}: {}", notification.getType(), channel,
                    e.getMessage());
            notification.registerFailure(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(),
                    maxAttempts);
        }
    }

    private static Notification base(Long beneficiaryId, Long loanId, NotificationType type, String message,
                                     LocalDate referenceDate, String waLink, NotificationChannel channel) {
        return Notification.builder()
                .recipientType(RecipientType.BENEFICIARIO)
                .beneficiaryId(beneficiaryId)
                .loanId(loanId)
                .channel(channel)
                .type(type)
                .message(message)
                .status(NotificationStatus.PENDENTE)
                .waLink(waLink)
                .referenceDate(referenceDate)
                .build();
    }

    private List<NotificationResponse> toResponses(List<Notification> notifications) {
        Map<Long, Beneficiary> cache = new HashMap<>();
        return notifications.stream()
                .map(n -> NotificationResponse.from(n, n.getBeneficiaryId() == null ? null
                        : cache.computeIfAbsent(n.getBeneficiaryId(),
                        id -> beneficiaryRepository.findById(id).orElse(null))))
                .toList();
    }
}
