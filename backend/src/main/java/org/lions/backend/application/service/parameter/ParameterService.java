package org.lions.backend.application.service.parameter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lions.backend.application.dto.response.SystemParameterResponse;
import org.lions.backend.application.port.CurrentUserPort;
import org.lions.backend.application.service.audit.AuditService;
import org.lions.backend.domain.entity.SystemParameter;
import org.lions.backend.domain.enums.AuditAction;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.exception.BusinessException;
import org.lions.backend.domain.exception.ResourceNotFoundException;
import org.lions.backend.domain.repository.SystemParameterRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Parâmetros de sistema (prazos e limites) com defaults. Todos os parâmetros conhecidos são inteiros positivos.
 * {@code loan.days.<TIPO>} sobrescreve o prazo padrão para um tipo de equipamento.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParameterService {

    public static final String ENTITY_NAME = "SystemParameter";

    public static final String LOAN_DEFAULT_DAYS = "loan.default-days";
    public static final String LOAN_DAYS_PREFIX = "loan.days.";
    public static final String LOAN_MAX_ACTIVE_PER_BENEFICIARY = "loan.max-active-per-beneficiary";
    public static final String LOAN_MAX_RENEWALS = "loan.max-renewals";
    public static final String LOAN_RENEWAL_WINDOW_DAYS = "loan.renewal-window-days";
    public static final String LOAN_DEFAULT_ESCALATION_DAYS = "loan.default-escalation-days";
    public static final String WAITLIST_RESERVATION_HOURS = "waitlist.reservation-hours";
    public static final String WAITLIST_MAX_CONSECUTIVE_EXPIRATIONS = "waitlist.max-consecutive-expirations";
    public static final String MAINTENANCE_PREVENTIVE_MONTHS = "maintenance.preventive-months";
    public static final String MAINTENANCE_PREVENTIVE_LOAN_CYCLES = "maintenance.preventive-loan-cycles";
    public static final String NOTIFICATION_MAX_ATTEMPTS = "notification.max-attempts";
    public static final String NOTIFICATION_OVERDUE_INTERVAL_DAYS = "notification.overdue-interval-days";
    public static final String REPORT_DEFAULT_PERIOD_DAYS = "report.default-period-days";

    /** Definições dos parâmetros fixos: chave → (default, descrição). */
    public static final Map<String, Definition> DEFINITIONS;

    static {
        Map<String, Definition> map = new LinkedHashMap<>();
        map.put(LOAN_DEFAULT_DAYS, new Definition(30, "Prazo padrão do empréstimo (dias)"));
        map.put(LOAN_MAX_ACTIVE_PER_BENEFICIARY, new Definition(2, "Máximo de empréstimos em aberto por beneficiário"));
        map.put(LOAN_MAX_RENEWALS, new Definition(2, "Máximo de renovações por empréstimo (RN23)"));
        map.put(LOAN_RENEWAL_WINDOW_DAYS, new Definition(7, "Renovação permitida a partir de N dias antes do vencimento (RN23)"));
        map.put(LOAN_DEFAULT_ESCALATION_DAYS, new Definition(30, "Dias de atraso para registrar inadimplência (RN27)"));
        map.put(WAITLIST_RESERVATION_HOURS, new Definition(48, "Prazo da reserva para o 1º da fila (horas, RN32)"));
        map.put(WAITLIST_MAX_CONSECUTIVE_EXPIRATIONS, new Definition(2, "Expirações/recusas consecutivas até remover da fila (RN33)"));
        map.put(MAINTENANCE_PREVENTIVE_MONTHS, new Definition(12, "Meses sem manutenção para sinalizar preventiva (RN39)"));
        map.put(MAINTENANCE_PREVENTIVE_LOAN_CYCLES, new Definition(10, "Ciclos de empréstimo para sinalizar preventiva (RN39)"));
        map.put(NOTIFICATION_MAX_ATTEMPTS, new Definition(3, "Tentativas de envio antes do contato manual (RN48)"));
        map.put(NOTIFICATION_OVERDUE_INTERVAL_DAYS, new Definition(3, "Intervalo (dias) entre avisos de atraso"));
        map.put(REPORT_DEFAULT_PERIOD_DAYS, new Definition(30, "Período padrão dos relatórios e dashboard (dias)"));
        DEFINITIONS = Collections.unmodifiableMap(map);
    }

    private final SystemParameterRepositoryPort repository;
    private final AuditService auditService;
    private final CurrentUserPort currentUser;
    private final Clock clock;

    public record Definition(int defaultValue, String description) {
    }

    /** Todos os parâmetros conhecidos (inclui {@code loan.days.<TIPO>}), com valor efetivo e default. */
    @Transactional(readOnly = true)
    public List<SystemParameterResponse> findAll() {
        List<SystemParameterResponse> result = new ArrayList<>();
        DEFINITIONS.forEach((key, definition) ->
                result.add(toResponse(key, definition.defaultValue(), definition.description())));
        int defaultDays = loanDefaultDays();
        for (EquipmentType type : EquipmentType.values()) {
            result.add(toResponse(LOAN_DAYS_PREFIX + type.name(), defaultDays,
                    "Prazo do empréstimo para " + type.getDescription() + " (dias; vazio = prazo padrão)"));
        }
        return result;
    }

    /** Atualiza um parâmetro conhecido; o valor deve ser inteiro positivo. Audita a alteração (RN58). */
    @Transactional
    public SystemParameterResponse update(String key, String value) {
        if (!isKnownKey(key)) {
            throw new ResourceNotFoundException("Parâmetro", key);
        }
        String normalized = value == null ? "" : value.trim();
        int parsed = parsePositive(normalized)
                .orElseThrow(() -> new BusinessException(
                        "O valor do parâmetro " + key + " deve ser um número inteiro positivo."));
        String newValue = String.valueOf(parsed);

        SystemParameter parameter = repository.findByKey(key)
                .orElseGet(() -> SystemParameter.builder().key(key).description(descriptionOf(key)).build());
        String oldValue = parameter.getValue();
        parameter.update(newValue, currentUser.username(), LocalDateTime.now(clock));
        repository.save(parameter);

        Map<String, Object> change = new LinkedHashMap<>();
        change.put(AuditService.OLD, oldValue);
        change.put(AuditService.NEW, newValue);
        auditService.record(ENTITY_NAME, key, AuditAction.UPDATE, Map.of("value", change));

        return toResponse(key, defaultOf(key), descriptionOf(key));
    }

    /** Valor efetivo (armazenado ou default) de uma chave conhecida. */
    @Transactional(readOnly = true)
    public int getInt(String key) {
        if (!isKnownKey(key)) {
            throw new IllegalArgumentException("Parâmetro desconhecido: " + key);
        }
        return storedValue(key).orElseGet(() -> defaultOf(key));
    }

    public int loanDefaultDays() {
        return getInt(LOAN_DEFAULT_DAYS);
    }

    /** Prazo para o tipo ({@code loan.days.<TIPO>}) ou o prazo padrão. */
    public int loanDays(EquipmentType type) {
        return storedValue(LOAN_DAYS_PREFIX + type.name()).orElseGet(this::loanDefaultDays);
    }

    public int maxActiveLoansPerBeneficiary() {
        return getInt(LOAN_MAX_ACTIVE_PER_BENEFICIARY);
    }

    public int maxRenewals() {
        return getInt(LOAN_MAX_RENEWALS);
    }

    public int renewalWindowDays() {
        return getInt(LOAN_RENEWAL_WINDOW_DAYS);
    }

    public int defaultEscalationDays() {
        return getInt(LOAN_DEFAULT_ESCALATION_DAYS);
    }

    public int reservationHours() {
        return getInt(WAITLIST_RESERVATION_HOURS);
    }

    public int maxConsecutiveExpirations() {
        return getInt(WAITLIST_MAX_CONSECUTIVE_EXPIRATIONS);
    }

    public int preventiveMonths() {
        return getInt(MAINTENANCE_PREVENTIVE_MONTHS);
    }

    public int preventiveLoanCycles() {
        return getInt(MAINTENANCE_PREVENTIVE_LOAN_CYCLES);
    }

    public int notificationMaxAttempts() {
        return getInt(NOTIFICATION_MAX_ATTEMPTS);
    }

    public int overdueIntervalDays() {
        return getInt(NOTIFICATION_OVERDUE_INTERVAL_DAYS);
    }

    public int reportDefaultPeriodDays() {
        return getInt(REPORT_DEFAULT_PERIOD_DAYS);
    }

    public static boolean isKnownKey(String key) {
        if (key == null) {
            return false;
        }
        if (DEFINITIONS.containsKey(key)) {
            return true;
        }
        if (key.startsWith(LOAN_DAYS_PREFIX)) {
            String type = key.substring(LOAN_DAYS_PREFIX.length());
            for (EquipmentType candidate : EquipmentType.values()) {
                if (candidate.name().equals(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Optional<Integer> storedValue(String key) {
        return repository.findByKey(key).flatMap(parameter -> {
            Optional<Integer> parsed = parsePositive(parameter.getValue());
            if (parsed.isEmpty()) {
                log.warn("Valor inválido para o parâmetro {}: '{}'. Usando o default.", key, parameter.getValue());
            }
            return parsed;
        });
    }

    private int defaultOf(String key) {
        Definition definition = DEFINITIONS.get(key);
        return definition != null ? definition.defaultValue() : loanDefaultDays();
    }

    private String descriptionOf(String key) {
        Definition definition = DEFINITIONS.get(key);
        if (definition != null) {
            return definition.description();
        }
        return "Prazo do empréstimo para " + key.substring(LOAN_DAYS_PREFIX.length()) + " (dias)";
    }

    private SystemParameterResponse toResponse(String key, int defaultValue, String description) {
        Optional<SystemParameter> stored = repository.findByKey(key);
        Optional<Integer> storedValue = stored.flatMap(parameter -> parsePositive(parameter.getValue()));
        return new SystemParameterResponse(
                key,
                String.valueOf(storedValue.orElse(defaultValue)),
                String.valueOf(defaultValue),
                description,
                storedValue.isPresent(),
                stored.map(SystemParameter::getUpdatedBy).orElse(null),
                stored.map(SystemParameter::getUpdatedAt).orElse(null));
    }

    private static Optional<Integer> parsePositive(String value) {
        if (value == null || !value.trim().matches("\\d{1,9}")) {
            return Optional.empty();
        }
        int parsed = Integer.parseInt(value.trim());
        return parsed > 0 ? Optional.of(parsed) : Optional.empty();
    }
}
