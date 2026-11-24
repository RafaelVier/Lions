package org.lions.backend.application.service.equipment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lions.backend.application.dto.request.EquipmentTypeUpdateRequest;
import org.lions.backend.application.dto.response.EquipmentTypeResponse;
import org.lions.backend.application.port.CurrentUserPort;
import org.lions.backend.application.service.audit.AuditService;
import org.lions.backend.application.service.parameter.ParameterService;
import org.lions.backend.domain.entity.EquipmentTypeConfig;
import org.lions.backend.domain.enums.AuditAction;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.exception.ResourceNotFoundException;
import org.lions.backend.domain.repository.EquipmentTypeConfigRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Tipos de equipamento (MER: tb_equipamento_tipo) — rótulo e prazo do empréstimo por tipo (RN14, RN18). */
@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentTypeService {

    public static final String ENTITY_NAME = "EquipmentType";

    private final EquipmentTypeConfigRepositoryPort repository;
    private final ParameterService parameterService;
    private final AuditService auditService;
    private final CurrentUserPort currentUser;
    private final Clock clock;

    /** Todos os tipos do enum, com o prazo efetivo (registros ausentes aparecem com os valores padrão). */
    @Transactional(readOnly = true)
    public List<EquipmentTypeResponse> findAll() {
        int defaultDays = parameterService.loanDefaultDays();
        return Arrays.stream(EquipmentType.values())
                .map(type -> repository.findByCode(type).orElseGet(() -> EquipmentTypeConfig.defaultFor(type)))
                .map(config -> toResponse(config, defaultDays))
                .toList();
    }

    /** Atualiza descrição e prazo do tipo (só Admin). Audita valores anteriores (RN58). */
    @Transactional
    public EquipmentTypeResponse update(String code, EquipmentTypeUpdateRequest request) {
        EquipmentType type = parse(code);
        EquipmentTypeConfig config = repository.findByCode(type).orElseGet(() -> EquipmentTypeConfig.defaultFor(type));
        String oldDescription = config.getDescription();
        Integer oldLoanDays = config.getLoanDays();
        config.update(request.description(), request.loanDays(), currentUser.username(), LocalDateTime.now(clock));
        EquipmentTypeConfig saved = repository.save(config);

        Map<String, Object> changes = new LinkedHashMap<>();
        if (!Objects.equals(oldDescription, saved.getDescription())) {
            changes.put("description", change(oldDescription, saved.getDescription()));
        }
        if (!Objects.equals(oldLoanDays, saved.getLoanDays())) {
            changes.put("loanDays", change(oldLoanDays, saved.getLoanDays()));
        }
        if (!changes.isEmpty()) {
            auditService.record(ENTITY_NAME, type.name(), AuditAction.UPDATE, changes);
        }
        return toResponse(saved, parameterService.loanDefaultDays());
    }

    /** Garante uma linha por tipo do enum (idempotente; usado na inicialização). */
    @Transactional
    public int ensureDefaults() {
        int created = 0;
        for (EquipmentType type : EquipmentType.values()) {
            if (repository.findByCode(type).isEmpty()) {
                repository.save(EquipmentTypeConfig.defaultFor(type));
                created++;
            }
        }
        if (created > 0) {
            log.info("{} tipo(s) de equipamento cadastrado(s) com os valores padrão.", created);
        }
        return created;
    }

    private static EquipmentType parse(String code) {
        if (code != null) {
            for (EquipmentType type : EquipmentType.values()) {
                if (type.name().equalsIgnoreCase(code.trim())) {
                    return type;
                }
            }
        }
        throw new ResourceNotFoundException("Tipo de equipamento", code);
    }

    private static Map<String, Object> change(Object oldValue, Object newValue) {
        Map<String, Object> change = new LinkedHashMap<>();
        change.put(AuditService.OLD, oldValue);
        change.put(AuditService.NEW, newValue);
        return change;
    }

    private static EquipmentTypeResponse toResponse(EquipmentTypeConfig config, int defaultDays) {
        return new EquipmentTypeResponse(config.getCode(), config.getDescription(), config.getLoanDays(),
                config.effectiveLoanDays(defaultDays), config.getUpdatedBy(), config.getUpdatedAt());
    }
}
