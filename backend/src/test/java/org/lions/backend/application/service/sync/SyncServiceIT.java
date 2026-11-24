package org.lions.backend.application.service.sync;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lions.backend.application.dto.request.EquipmentCreateRequest;
import org.lions.backend.application.dto.request.LoanCreateRequest;
import org.lions.backend.application.dto.request.SyncBatchRequest;
import org.lions.backend.application.dto.request.SyncOperationRequest;
import org.lions.backend.application.dto.response.LoanResponse;
import org.lions.backend.application.dto.response.SyncConflictResponse;
import org.lions.backend.application.dto.response.SyncOperationResult;
import org.lions.backend.application.service.beneficiary.BeneficiaryService;
import org.lions.backend.application.service.equipment.EquipmentService;
import org.lions.backend.application.service.loan.LoanService;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.LoanStatus;
import org.lions.backend.domain.enums.NotificationType;
import org.lions.backend.domain.enums.ReturnCondition;
import org.lions.backend.domain.enums.SyncConflictStatus;
import org.lions.backend.domain.enums.SyncOperationType;
import org.lions.backend.domain.enums.SyncPrevailing;
import org.lions.backend.domain.enums.SyncResultStatus;
import org.lions.backend.domain.exception.BusinessException;
import org.lions.backend.domain.repository.NotificationRepositoryPort;
import org.lions.backend.support.TestFixtures;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** G.14 — sincronização offline (RN60). */
@SpringBootTest
@Transactional
class SyncServiceIT {

    @Autowired
    private SyncService syncService;
    @Autowired
    private BeneficiaryService beneficiaryService;
    @Autowired
    private EquipmentService equipmentService;
    @Autowired
    private LoanService loanService;
    @Autowired
    private NotificationRepositoryPort notificationRepository;

    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void authenticate() {
        TestFixtures.authenticateAs("operador", "OPERATOR");
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private Long beneficiary(String name) {
        return beneficiaryService.create(TestFixtures.beneficiaryRequest(name)).id();
    }

    private Long equipment() {
        return equipmentService.create(EquipmentCreateRequest.builder()
                .type(EquipmentType.ANDADOR).condition(EquipmentCondition.BOM).build()).getId();
    }

    private static SyncOperationRequest checkout(String id, LocalDateTime at, Long beneficiaryId, Long equipmentId) {
        return new SyncOperationRequest(id, SyncOperationType.RETIRADA, at, beneficiaryId, equipmentId, null, null,
                null, "Entregue na casa do beneficiário");
    }

    private static SyncOperationRequest giveBack(String id, LocalDateTime at, Long loanId) {
        return new SyncOperationRequest(id, SyncOperationType.DEVOLUCAO, at, null, null, loanId,
                ReturnCondition.BOM_ESTADO, null, null);
    }

    private static SyncOperationRequest renewal(String id, LocalDateTime at, Long loanId) {
        return new SyncOperationRequest(id, SyncOperationType.RENOVACAO, at, null, null, loanId, null, null, null);
    }

    private List<SyncOperationResult> sync(SyncOperationRequest... ops) {
        return syncService.process(new SyncBatchRequest(List.of(ops)));
    }

    private static String id() {
        return UUID.randomUUID().toString();
    }

    @Test
    void deveAplicarRetiradaOfflineComDataDoDispositivoENaoDuplicarQuandoReenviada() {
        Long b = beneficiary("Offline A");
        Long e = equipment();
        SyncOperationRequest op = checkout(id(), now.minusDays(2), b, e);

        SyncOperationResult first = sync(op).get(0);
        assertThat(first.status()).isEqualTo(SyncResultStatus.APLICADA);
        LoanResponse loan = loanService.findById(first.loanId());
        assertThat(loan.getLoanDate()).isEqualTo(LocalDate.now().minusDays(2));
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.PENDENTE_ASSINATURA);
        assertThat(loan.isRetroactive()).isTrue();

        SyncOperationResult again = sync(op).get(0);
        assertThat(again.status()).isEqualTo(SyncResultStatus.DUPLICADA);
        assertThat(again.loanId()).isEqualTo(first.loanId());
        assertThat(loanService.findAll(new LoanService.LoanFilter(null, b, null, null, null, null))).hasSize(1);
    }

    @Test
    void deveRegistrarConflitoEPrevalecerMaisAntigoQuandoMesmoEquipamento() {
        Long first = beneficiary("Primeiro");
        Long second = beneficiary("Segundo");
        Long third = beneficiary("Terceiro");
        Long e = equipment();
        sync(checkout(id(), now.minusHours(3), first, e));

        // outro dispositivo retirou o mesmo item depois → prevalece o registro existente (mais antigo)
        SyncOperationResult later = sync(checkout(id(), now.minusHours(1), second, e)).get(0);
        assertThat(later.status()).isEqualTo(SyncResultStatus.CONFLITO);
        SyncConflictResponse c1 = syncService.findConflict(later.conflictId());
        assertThat(c1.prevailing()).isEqualTo(SyncPrevailing.EXISTENTE);
        assertThat(c1.status()).isEqualTo(SyncConflictStatus.PENDENTE);
        assertThat(c1.equipmentId()).isEqualTo(e);

        // operação offline mais antiga que o registro existente → indicada para prevalecer (correção manual)
        SyncOperationResult earlier = sync(checkout(id(), now.minusHours(5), third, e)).get(0);
        assertThat(earlier.status()).isEqualTo(SyncResultStatus.CONFLITO);
        assertThat(syncService.findConflict(earlier.conflictId()).prevailing()).isEqualTo(SyncPrevailing.RECEBIDA);
        assertThat(earlier.message()).contains("corrija manualmente");

        assertThat(notificationRepository.findAll(null, NotificationType.CONFLITO_SINCRONIZACAO)).hasSizeGreaterThanOrEqualTo(2);
        assertThat(syncService.findConflicts(SyncConflictStatus.PENDENTE)).extracting(SyncConflictResponse::id)
                .contains(later.conflictId(), earlier.conflictId());

        TestFixtures.authenticateAs("admin", "ADMIN");
        SyncConflictResponse resolved = syncService.resolve(later.conflictId(), "Conferido: segundo registro descartado.");
        assertThat(resolved.status()).isEqualTo(SyncConflictStatus.RESOLVIDO);
        assertThat(resolved.resolvedBy()).isEqualTo("admin");
        assertThatThrownBy(() -> syncService.resolve(later.conflictId(), "de novo"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deveAplicarEmOrdemCronologicaERecusarSemDesfazerAsDemais() {
        Long b = beneficiary("Ordem");
        Long e = equipment();
        LoanResponse loan = loanService.registerLoan(LoanCreateRequest.builder().beneficiaryId(b).equipmentId(e).build());
        loanService.registerDigitalSignature(loan.getId(),
                new org.lions.backend.application.dto.request.DigitalSignatureRequest(null));

        String returnId = id();
        // enviado fora de ordem: a renovação (mais antiga) é aplicada primeiro e recusada (fora da janela da RN23)
        List<SyncOperationResult> results = sync(giveBack(returnId, now.minusMinutes(10), loan.getId()),
                renewal(id(), now.minusMinutes(30), loan.getId()));

        assertThat(results).extracting(SyncOperationResult::type)
                .containsExactly(SyncOperationType.RENOVACAO, SyncOperationType.DEVOLUCAO);
        assertThat(results.get(0).status()).isEqualTo(SyncResultStatus.RECUSADA);
        assertThat(results.get(1).status()).isEqualTo(SyncResultStatus.APLICADA);
        assertThat(loanService.findById(loan.getId()).getStatus()).isEqualTo(LoanStatus.DEVOLVIDO);

        // outra devolução do mesmo empréstimo feita depois, em outro aparelho → conflito; prevalece a primeira
        SyncOperationResult duplicateReturn = sync(giveBack(id(), now.minusMinutes(5), loan.getId())).get(0);
        assertThat(duplicateReturn.status()).isEqualTo(SyncResultStatus.CONFLITO);
        assertThat(syncService.findConflict(duplicateReturn.conflictId()).prevailing())
                .isEqualTo(SyncPrevailing.EXISTENTE);
    }

    @Test
    void deveRecusarQuandoFaltamCamposDaOperacao() {
        SyncOperationResult result = sync(new SyncOperationRequest(id(), SyncOperationType.DEVOLUCAO, now, null, null,
                null, null, null, null)).get(0);
        assertThat(result.status()).isEqualTo(SyncResultStatus.RECUSADA);
        assertThat(result.message()).contains("loanId");
    }
}
