package org.lions.backend.application.service.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lions.backend.application.port.CurrentUserPort;
import org.lions.backend.domain.entity.AuditLog;
import org.lions.backend.domain.enums.AuditAction;
import org.lions.backend.domain.repository.AuditLogRepositoryPort;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.PropertyDescriptor;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Trilha de auditoria (RN25/RN52/RN58). {@code details} é serializado como JSON;
 * para alterações use {@link #diff(Object, Object, String...)} → {@code {campo: {old, new}}}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    public static final String OLD = "old";
    public static final String NEW = "new";
    private static final Set<String> IGNORED_PROPERTIES = Set.of("class", "createdAt", "updatedAt", "version");

    private final AuditLogRepositoryPort auditLogRepository;
    private final CurrentUserPort currentUser;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    /** Registra ação do usuário corrente. */
    @Transactional
    public AuditLog record(String entityName, Object entityId, AuditAction action, Object details) {
        return persist(entityName, entityId, action, currentUser.username(), false, null, details);
    }

    /** Registra ação automática do sistema (username {@value CurrentUserPort#SYSTEM_USERNAME}) com a RN de origem. */
    @Transactional
    public AuditLog recordAutomatic(String entityName, Object entityId, AuditAction action, String originRule,
                                    Object details) {
        return persist(entityName, entityId, action, CurrentUserPort.SYSTEM_USERNAME, true, originRule, details);
    }

    /**
     * Compara as propriedades de {@code before} e {@code after} e devolve apenas as alteradas:
     * {@code {campo: {"old": valorAntigo, "new": valorNovo}}} (ordem preservada).
     * Sem {@code fields}, compara todas as propriedades de tipo simples (exceto createdAt/updatedAt/version).
     */
    public Map<String, Map<String, Object>> diff(Object before, Object after, String... fields) {
        Map<String, Map<String, Object>> changes = new LinkedHashMap<>();
        if (before == null && after == null) {
            return changes;
        }
        BeanWrapper oldWrapper = before == null ? null : new BeanWrapperImpl(before);
        BeanWrapper newWrapper = after == null ? null : new BeanWrapperImpl(after);
        List<String> properties = fields != null && fields.length > 0
                ? Arrays.asList(fields)
                : simpleProperties(after != null ? newWrapper : oldWrapper);
        for (String property : properties) {
            Object oldValue = read(oldWrapper, property);
            Object newValue = read(newWrapper, property);
            if (!sameValue(oldValue, newValue)) {
                Map<String, Object> change = new LinkedHashMap<>();
                change.put(OLD, oldValue);
                change.put(NEW, newValue);
                changes.put(property, change);
            }
        }
        return changes;
    }

    @Transactional(readOnly = true)
    public List<AuditLog> search(String entityName, String entityId, LocalDateTime from, LocalDateTime to) {
        return auditLogRepository.search(entityName, entityId, from, to);
    }

    private AuditLog persist(String entityName, Object entityId, AuditAction action, String username,
                             boolean automatic, String originRule, Object details) {
        AuditLog entry = AuditLog.builder()
                .entityName(entityName)
                .entityId(entityId == null ? null : String.valueOf(entityId))
                .action(action)
                .username(username)
                .automatic(automatic)
                .originRule(originRule)
                .details(serialize(details))
                .occurredAt(LocalDateTime.now(clock))
                .build();
        return auditLogRepository.save(entry);
    }

    private String serialize(Object details) {
        if (details == null) {
            return null;
        }
        if (details instanceof CharSequence text) {
            return text.toString();
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            log.warn("Falha ao serializar detalhes de auditoria: {}", e.getMessage());
            return String.valueOf(details);
        }
    }

    private static List<String> simpleProperties(BeanWrapper wrapper) {
        List<String> names = new ArrayList<>();
        for (PropertyDescriptor descriptor : wrapper.getPropertyDescriptors()) {
            if (descriptor.getReadMethod() != null
                    && !IGNORED_PROPERTIES.contains(descriptor.getName())
                    && BeanUtils.isSimpleValueType(descriptor.getPropertyType())) {
                names.add(descriptor.getName());
            }
        }
        return names;
    }

    private static Object read(BeanWrapper wrapper, String property) {
        if (wrapper == null || !wrapper.isReadableProperty(property)) {
            return null;
        }
        return wrapper.getPropertyValue(property);
    }

    private static boolean sameValue(Object a, Object b) {
        if (a instanceof BigDecimal x && b instanceof BigDecimal y) {
            return x.compareTo(y) == 0;
        }
        return Objects.equals(a, b);
    }
}
