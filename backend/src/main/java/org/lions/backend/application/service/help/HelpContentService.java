package org.lions.backend.application.service.help;

import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.request.HelpContentRequest;
import org.lions.backend.application.dto.response.HelpContentResponse;
import org.lions.backend.application.port.CurrentUserPort;
import org.lions.backend.application.service.audit.AuditService;
import org.lions.backend.application.support.AuditDetails;
import org.lions.backend.domain.entity.HelpContent;
import org.lions.backend.domain.enums.AuditAction;
import org.lions.backend.domain.enums.HelpAudience;
import org.lions.backend.domain.enums.HelpContentType;
import org.lions.backend.domain.enums.UserRole;
import org.lions.backend.domain.exception.ResourceNotFoundException;
import org.lions.backend.domain.repository.HelpContentRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Ajuda/onboarding (RF14 — RN53): conteúdo mantido pelo Admin e filtrado pelo perfil; FAQ pública sem login. */
@Service
@RequiredArgsConstructor
public class HelpContentService {

    public static final String ENTITY_NAME = "HelpContent";

    private final HelpContentRepositoryPort helpRepository;
    private final AuditService auditService;
    private final CurrentUserPort currentUser;

    /** Público (sem login): conteúdos ativos com audiência PUBLICO. */
    @Transactional(readOnly = true)
    public List<HelpContentResponse> findPublic(HelpContentType type) {
        return visibleTo(Set.of(HelpAudience.PUBLICO)).stream()
                .filter(h -> type == null || h.getType() == type)
                .map(HelpContentResponse::from)
                .toList();
    }

    /**
     * Conteúdos visíveis ao perfil do usuário (mais os públicos). {@code all=true} (somente Admin) lista tudo,
     * inclusive inativos, para gestão.
     */
    @Transactional(readOnly = true)
    public List<HelpContentResponse> findForCurrentUser(HelpContentType type, boolean all) {
        List<HelpContent> contents = all && currentUser.hasRole(UserRole.ROLE_ADMIN)
                ? helpRepository.findAll()
                : visibleTo(currentAudiences());
        return contents.stream()
                .filter(h -> type == null || h.getType() == type)
                .map(HelpContentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public HelpContentResponse findById(Long id) {
        return HelpContentResponse.from(find(id));
    }

    @Transactional
    public HelpContentResponse create(HelpContentRequest request) {
        HelpContent content = HelpContent.builder().active(true).build();
        apply(content, request);
        content.validate();
        HelpContent saved = helpRepository.save(content);
        auditService.record(ENTITY_NAME, saved.getId(), AuditAction.CREATE, AuditDetails.of(
                "type", saved.getType(), "title", saved.getTitle(), "audiences", saved.getAudiences()));
        return HelpContentResponse.from(saved);
    }

    @Transactional
    public HelpContentResponse update(Long id, HelpContentRequest request) {
        HelpContent content = find(id);
        HelpContent before = find(id);
        apply(content, request);
        content.validate();
        HelpContent saved = helpRepository.save(content);
        var changes = auditService.diff(before, saved, "type", "title", "content", "keywords", "displayOrder");
        if (!before.getAudiences().equals(saved.getAudiences())) {
            Map<String, Object> audiences = new LinkedHashMap<>();
            audiences.put(AuditService.OLD, before.getAudiences());
            audiences.put(AuditService.NEW, saved.getAudiences());
            changes.put("audiences", audiences);
        }
        if (!changes.isEmpty()) {
            auditService.record(ENTITY_NAME, id, AuditAction.UPDATE, changes);
        }
        return HelpContentResponse.from(saved);
    }

    @Transactional
    public HelpContentResponse setActive(Long id, boolean active) {
        HelpContent content = find(id);
        if (content.isActive() == active) {
            return HelpContentResponse.from(content);
        }
        if (active) {
            content.setActive(true);
        } else {
            content.inactivate();
        }
        HelpContent saved = helpRepository.save(content);
        auditService.record(ENTITY_NAME, id, active ? AuditAction.REACTIVATE : AuditAction.INACTIVATE,
                AuditDetails.of("active", AuditDetails.oldNew(!active, active)));
        return HelpContentResponse.from(saved);
    }

    /** Conteúdos ativos visíveis às audiências, sem repetição, ordenados por {@code displayOrder}. */
    @Transactional(readOnly = true)
    public List<HelpContent> visibleTo(Set<HelpAudience> audiences) {
        Map<Long, HelpContent> unique = new LinkedHashMap<>();
        for (HelpAudience audience : audiences) {
            helpRepository.findActiveByAudience(audience).forEach(h -> unique.putIfAbsent(h.getId(), h));
        }
        return unique.values().stream()
                .sorted(Comparator.comparingInt(HelpContent::getDisplayOrder).thenComparing(HelpContent::getId))
                .toList();
    }

    /** Audiências do usuário corrente: PUBLICO + a do perfil (anônimo = só PUBLICO). */
    public Set<HelpAudience> currentAudiences() {
        Set<HelpAudience> audiences = EnumSet.of(HelpAudience.PUBLICO);
        if (currentUser.hasRole(UserRole.ROLE_ADMIN)) {
            audiences.add(HelpAudience.ADMIN);
        }
        if (currentUser.hasRole(UserRole.ROLE_OPERATOR)) {
            audiences.add(HelpAudience.OPERADOR);
        }
        if (currentUser.hasRole(UserRole.ROLE_VIEWER)) {
            audiences.add(HelpAudience.CONSULTA);
        }
        return audiences;
    }

    private static void apply(HelpContent content, HelpContentRequest request) {
        content.setType(request.type());
        content.setTitle(request.title().trim());
        content.setContent(request.content().trim());
        content.setKeywords(request.keywords() == null || request.keywords().isBlank() ? null
                : request.keywords().trim());
        Set<HelpAudience> audiences = EnumSet.noneOf(HelpAudience.class);
        audiences.addAll(request.audiences());
        content.setAudiences(audiences);
        content.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
    }

    private HelpContent find(Long id) {
        return helpRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Conteúdo de ajuda", id));
    }
}
