package org.lions.backend.application.service.loan;

import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.response.OccurrenceResponse;
import org.lions.backend.application.port.CurrentUserPort;
import org.lions.backend.application.service.audit.AuditService;
import org.lions.backend.application.support.AuditDetails;
import org.lions.backend.domain.entity.Occurrence;
import org.lions.backend.domain.enums.AuditAction;
import org.lions.backend.domain.enums.OccurrenceStatus;
import org.lions.backend.domain.enums.OccurrenceType;
import org.lions.backend.domain.exception.ResourceNotFoundException;
import org.lions.backend.domain.repository.OccurrenceRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Ocorrências de dano (RN40) e inadimplência (RN27); ocorrência aberta bloqueia o beneficiário (RN20). */
@Service
@RequiredArgsConstructor
public class OccurrenceService {

    public static final String ENTITY_NAME = "Occurrence";

    private final OccurrenceRepositoryPort occurrenceRepository;
    private final AuditService auditService;
    private final CurrentUserPort currentUser;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<OccurrenceResponse> findAll(Long beneficiaryId, OccurrenceStatus status, OccurrenceType type) {
        List<Occurrence> occurrences;
        if (beneficiaryId != null) {
            occurrences = occurrenceRepository.findByBeneficiaryId(beneficiaryId);
        } else if (status != null) {
            occurrences = occurrenceRepository.findByStatus(status);
        } else {
            occurrences = occurrenceRepository.findAll();
        }
        return occurrences.stream()
                .filter(o -> status == null || o.getStatus() == status)
                .filter(o -> type == null || o.getType() == type)
                .sorted(Comparator.comparing(Occurrence::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(OccurrenceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OccurrenceResponse findById(Long id) {
        return OccurrenceResponse.from(find(id));
    }

    /** Resolve a ocorrência (somente Admin — verificado no controller), desbloqueando o beneficiário (RN20). */
    @Transactional
    public OccurrenceResponse resolve(Long id, String note) {
        Occurrence occurrence = find(id);
        occurrence.resolve(note, currentUser.username(), LocalDateTime.now(clock));
        Occurrence saved = occurrenceRepository.save(occurrence);
        auditService.record(ENTITY_NAME, id, AuditAction.STATUS_CHANGE, AuditDetails.of(
                "status", AuditDetails.oldNew(OccurrenceStatus.ABERTA, saved.getStatus()),
                "resolutionNote", note));
        return OccurrenceResponse.from(saved);
    }

    private Occurrence find(Long id) {
        return occurrenceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ocorrência", id));
    }
}
