package org.lions.backend.application.service.assistant;

import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.response.AllocationSuggestionsResponse;
import org.lions.backend.application.dto.response.AllocationSuggestionsResponse.Attention;
import org.lions.backend.application.dto.response.AllocationSuggestionsResponse.Suggestion;
import org.lions.backend.application.dto.response.AllocationSuggestionsResponse.TypeSummary;
import org.lions.backend.application.dto.response.AssistantAnswerResponse;
import org.lions.backend.application.dto.response.BeneficiaryRestriction;
import org.lions.backend.application.service.help.HelpContentService;
import org.lions.backend.application.service.loan.BeneficiaryRestrictionService;
import org.lions.backend.domain.entity.Equipment;
import org.lions.backend.domain.entity.HelpContent;
import org.lions.backend.domain.entity.LoanOrder;
import org.lions.backend.domain.entity.WaitlistEntry;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.HelpContentType;
import org.lions.backend.domain.enums.OrderStatus;
import org.lions.backend.domain.repository.EquipmentRepositoryPort;
import org.lions.backend.domain.repository.LoanOrderRepositoryPort;
import org.lions.backend.domain.repository.WaitlistEntryRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Assistente baseado em regras (RF13 parcial): sugestões de alocação (RN50 — só sugere, nunca executa; RN51 — só
 * Admin) e busca na base de ajuda/FAQ (RN51 — sem dados pessoais; anônimo vê apenas conteúdo público).
 */
@Service
@RequiredArgsConstructor
public class AssistantService {

    private static final int MAX_ANSWERS = 5;
    private static final Set<String> STOPWORDS = Set.of(
            "a", "o", "as", "os", "um", "uma", "de", "da", "do", "das", "dos", "e", "em", "no", "na", "nos", "nas",
            "para", "pra", "por", "com", "sem", "que", "qual", "quais", "como", "quando", "onde", "eu", "meu",
            "minha", "se", "ao", "aos", "ou", "posso", "pode", "preciso", "sobre", "ser", "tem", "ter", "voce");

    private final EquipmentRepositoryPort equipmentRepository;
    private final WaitlistEntryRepositoryPort waitlistRepository;
    private final LoanOrderRepositoryPort orderRepository;
    private final BeneficiaryRestrictionService restrictionService;
    private final HelpContentService helpContentService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AllocationSuggestionsResponse allocationSuggestions() {
        List<Suggestion> suggestions = new ArrayList<>();
        List<Attention> attention = new ArrayList<>();
        List<TypeSummary> summary = new ArrayList<>();
        List<LoanOrder> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDENTE_ANALISE).stream()
                .sorted(Comparator.comparing(LoanOrder::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        for (EquipmentType type : EquipmentType.values()) {
            Deque<Equipment> available = new ArrayDeque<>(equipmentRepository
                    .findByTypeAndStatus(type, EquipmentStatus.DISPONIVEL).stream()
                    .sorted(Comparator.comparing(Equipment::getId)).toList());
            long reserved = equipmentRepository.countByTypeAndStatus(type, EquipmentStatus.RESERVADO);
            List<WaitlistEntry> waiting = waitlistRepository.findActiveByType(type);
            List<LoanOrder> pendingOfType = pendingOrders.stream().filter(o -> o.getEquipmentType() == type).toList();
            summary.add(new TypeSummary(type, type.getDescription(), available.size(), reserved, waiting.size(),
                    pendingOfType.size()));

            int position = 0;
            for (WaitlistEntry entry : waiting) {
                position++;
                BeneficiaryRestriction restriction = restrictionService.evaluate(entry.getBeneficiary().getId());
                if (restriction.blocked()) {
                    attention.add(new Attention(entry.getId(), entry.getBeneficiary().getId(),
                            entry.getBeneficiary().getFullName(), type, restriction.reasons()));
                    continue;
                }
                if (available.isEmpty()) {
                    continue;
                }
                Equipment equipment = available.poll();
                suggestions.add(new Suggestion(type, type.getDescription(), equipment.getId(),
                        equipment.getPatrimonyNumber(), "FILA", entry.getId(), entry.getOrderId(),
                        entry.getBeneficiary().getId(), entry.getBeneficiary().getFullName(), entry.isPriority(),
                        position, entry.isPriority()
                        ? "Prioridade na fila (" + entry.getPriorityReason().getDescription() + ")."
                        : position + "º da fila de espera."));
            }
            for (LoanOrder order : pendingOfType) {
                if (available.isEmpty()) {
                    break;
                }
                BeneficiaryRestriction restriction = restrictionService.evaluate(order.getBeneficiary().getId());
                if (restriction.blocked()) {
                    continue;
                }
                Equipment equipment = available.poll();
                suggestions.add(new Suggestion(type, type.getDescription(), equipment.getId(),
                        equipment.getPatrimonyNumber(), "SOLICITACAO", null, order.getId(),
                        order.getBeneficiary().getId(), order.getBeneficiary().getFullName(), false, null,
                        "Solicitação pendente de análise (aprovar com este item)."));
            }
        }
        return new AllocationSuggestionsResponse(LocalDateTime.now(clock),
                "Sugestões consultivas: nenhuma ação é executada automaticamente. Confirme cada alocação pelos fluxos "
                        + "de empréstimo ou de aprovação de solicitação.",
                suggestions, attention, summary);
    }

    /** Busca por palavras-chave na base de ajuda visível ao usuário (anônimo: somente conteúdo público). */
    @Transactional(readOnly = true)
    public AssistantAnswerResponse answer(String question) {
        Set<String> terms = terms(question);
        if (terms.isEmpty()) {
            return new AssistantAnswerResponse(question, List.of(),
                    "Escreva sua dúvida com algumas palavras, por exemplo: \"como devolver o equipamento\".");
        }
        List<AssistantAnswerResponse.Answer> answers = helpContentService
                .visibleTo(helpContentService.currentAudiences()).stream()
                .map(content -> new AssistantAnswerResponse.Answer(content.getId(), content.getType(),
                        content.getTitle(), content.getContent(), score(content, terms)))
                .filter(answer -> answer.score() > 0)
                .sorted(Comparator.comparingInt(AssistantAnswerResponse.Answer::score).reversed()
                        .thenComparing(answer -> answer.type() == HelpContentType.FAQ ? 0 : 1))
                .limit(MAX_ANSWERS)
                .toList();
        String message = answers.isEmpty()
                ? "Não encontrei uma resposta para sua dúvida. Entre em contato com o Lions Clube de Medianeira."
                : null;
        return new AssistantAnswerResponse(question, answers, message);
    }

    private static int score(HelpContent content, Set<String> terms) {
        Set<String> title = terms(content.getTitle());
        Set<String> keywords = terms(content.getKeywords());
        Set<String> body = terms(content.getContent());
        int score = 0;
        for (String term : terms) {
            if (title.contains(term)) {
                score += 3;
            }
            if (keywords.contains(term)) {
                score += 2;
            }
            if (body.contains(term)) {
                score += 1;
            }
        }
        return score;
    }

    /** Normaliza (minúsculas, sem acentos), separa em palavras e remove stopwords/termos curtos; aplica radical simples. */
    private static Set<String> terms(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return Arrays.stream(normalized.split("[^a-z0-9]+"))
                .filter(word -> word.length() >= 3)
                .filter(word -> !STOPWORDS.contains(word))
                .map(AssistantService::stem)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Radical simples para plurais/variações comuns (devolver/devolução → devol). */
    private static String stem(String word) {
        String[] suffixes = {"coes", "cao", "mentos", "mento", "ndo", "res", "ver", "ar", "er", "ir", "es", "s"};
        for (String suffix : suffixes) {
            if (word.length() > suffix.length() + 3 && word.endsWith(suffix)) {
                return word.substring(0, word.length() - suffix.length());
            }
        }
        return word;
    }
}
