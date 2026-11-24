package org.lions.backend.domain.entity;

import org.junit.jupiter.api.Test;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.LoanStatus;
import org.lions.backend.domain.enums.SignatureType;
import org.lions.backend.domain.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanTest {

    private static final LocalDate TODAY = LocalDate.of(2025, 3, 10);
    private static final LocalDateTime NOW = TODAY.atTime(10, 0);
    private static final RenewalPolicy POLICY = new RenewalPolicy(2, 7);

    private static Loan activeLoan(LocalDate dueDate) {
        return Loan.builder()
                .id(1L)
                .beneficiary(Beneficiary.builder().id(1L).fullName("Maria").build())
                .equipment(Equipment.builder().id(2L).type(EquipmentType.ANDADOR).build())
                .status(LoanStatus.ATIVO)
                .loanDate(dueDate.minusDays(30))
                .initialExpectedReturnDate(dueDate)
                .expectedReturnDate(dueDate)
                .build();
    }

    private static Loan newLoan(LocalDate loanDate, LocalDate due, String justification) {
        return Loan.builder()
                .beneficiary(Beneficiary.builder().id(1L).build())
                .equipment(Equipment.builder().id(2L).build())
                .loanDate(loanDate)
                .expectedReturnDate(due)
                .retroactiveJustification(justification)
                .build();
    }

    // ---------- criação (RN22) ----------

    @Test
    void deveIniciarPendenteDeAssinaturaQuandoCriado() {
        Loan loan = newLoan(TODAY, TODAY.plusDays(30), null);
        loan.validateNew(TODAY);
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.PENDENTE_ASSINATURA);
        assertThat(loan.isRetroactive()).isFalse();
        assertThat(loan.getInitialExpectedReturnDate()).isEqualTo(TODAY.plusDays(30));
    }

    @Test
    void deveRejeitarQuandoDataDeRetiradaFutura() {
        assertThatThrownBy(() -> newLoan(TODAY.plusDays(1), TODAY.plusDays(31), null).validateNew(TODAY))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deveExigirJustificativaQuandoRetroativo() {
        assertThatThrownBy(() -> newLoan(TODAY.minusDays(3), TODAY.plusDays(27), " ").validateNew(TODAY))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("justificativa");

        Loan loan = newLoan(TODAY.minusDays(3), TODAY.plusDays(27), "Entregue durante queda do sistema");
        loan.validateNew(TODAY);
        assertThat(loan.isRetroactive()).isTrue();
    }

    @Test
    void deveRejeitarQuandoDevolucaoPrevistaNaoPosteriorARetirada() {
        assertThatThrownBy(() -> newLoan(TODAY, TODAY, null).validateNew(TODAY))
                .isInstanceOf(BusinessException.class);
    }

    // ---------- assinatura (RN21) ----------

    @Test
    void deveAtivarQuandoAssinaturaDigitalRegistrada() {
        Loan loan = newLoan(TODAY, TODAY.plusDays(30), null);
        loan.registerSignature(SignatureType.DIGITAL, "Maria", null, "operador", NOW);
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ATIVO);
        assertThat(loan.getSignedAt()).isEqualTo(NOW);
        assertThat(loan.getSignedByName()).isEqualTo("Maria");
        assertThat(loan.getSignatureRegisteredBy()).isEqualTo("operador");
    }

    @Test
    void deveExigirDocumentoQuandoAssinaturaFisica() {
        Loan loan = newLoan(TODAY, TODAY.plusDays(30), null);
        assertThatThrownBy(() -> loan.registerSignature(SignatureType.FISICA_DIGITALIZADA, "Maria", null, "op", NOW))
                .isInstanceOf(BusinessException.class);

        loan.registerSignature(SignatureType.FISICA_DIGITALIZADA, "Maria", "termos/1/assinado.pdf", "op", NOW);
        assertThat(loan.getSignedDocumentPath()).isEqualTo("termos/1/assinado.pdf");
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ATIVO);
    }

    @Test
    void deveRejeitarAssinaturaQuandoJaAtivoOuSemNome() {
        Loan active = activeLoan(TODAY.plusDays(10));
        assertThatThrownBy(() -> active.registerSignature(SignatureType.DIGITAL, "Maria", null, "op", NOW))
                .isInstanceOf(BusinessException.class);
        Loan pending = newLoan(TODAY, TODAY.plusDays(30), null);
        assertThatThrownBy(() -> pending.registerSignature(SignatureType.DIGITAL, " ", null, "op", NOW))
                .isInstanceOf(BusinessException.class);
    }

    // ---------- renovação (RN23) ----------

    @Test
    void deveRenovarQuandoDentroDaJanelaSemFilaEAbaixoDoLimite() {
        LocalDate due = TODAY.plusDays(7);
        Loan loan = activeLoan(due);

        LoanRenewal renewal = loan.renew(due.plusDays(30), TODAY, POLICY, false, "op", NOW);

        assertThat(loan.getExpectedReturnDate()).isEqualTo(due.plusDays(30));
        assertThat(loan.getInitialExpectedReturnDate()).isEqualTo(due);
        assertThat(loan.getRenewalCount()).isEqualTo(1);
        assertThat(loan.getLastRenewalDate()).isEqualTo(TODAY);
        assertThat(renewal.getPreviousDueDate()).isEqualTo(due);
        assertThat(renewal.getNewDueDate()).isEqualTo(due.plusDays(30));
        assertThat(renewal.getLoanId()).isEqualTo(1L);
        assertThat(renewal.getRenewedBy()).isEqualTo("op");
        assertThat(renewal.getRenewedAt()).isEqualTo(NOW);
    }

    @Test
    void deveRenovarQuandoNoDiaDoVencimento() {
        Loan loan = activeLoan(TODAY);
        assertThat(loan.canRenew(TODAY, POLICY, false)).isTrue();
    }

    @Test
    void deveBloquearRenovacaoQuandoAntesDaJanelaDe7Dias() {
        Loan loan = activeLoan(TODAY.plusDays(8));
        assertThat(loan.canRenew(TODAY, POLICY, false)).isFalse();
        assertThat(loan.renewalBlockers(TODAY, POLICY, false)).anyMatch(r -> r.contains("a partir de 11/03/2025"));
        assertThatThrownBy(() -> loan.renew(TODAY.plusDays(38), TODAY, POLICY, false, "op", NOW))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deveBloquearRenovacaoQuandoLimiteAtingido() {
        Loan loan = activeLoan(TODAY.plusDays(3));
        loan.setRenewalCount(2);
        assertThat(loan.renewalBlockers(TODAY, POLICY, false)).anyMatch(r -> r.contains("Limite de 2"));
        assertThatThrownBy(() -> loan.renew(TODAY.plusDays(33), TODAY, POLICY, false, "op", NOW))
                .isInstanceOf(BusinessException.class);
        assertThat(loan.getRenewalCount()).isEqualTo(2);
    }

    @Test
    void deveBloquearRenovacaoQuandoAtrasado() {
        Loan overdueStatus = activeLoan(TODAY.minusDays(1));
        overdueStatus.setStatus(LoanStatus.ATRASADO);
        assertThat(overdueStatus.renewalBlockers(TODAY, POLICY, false)).anyMatch(r -> r.contains("atraso"));

        Loan overdueNotMarked = activeLoan(TODAY.minusDays(1));
        assertThat(overdueNotMarked.canRenew(TODAY, POLICY, false)).isFalse();
    }

    @Test
    void deveBloquearRenovacaoQuandoHaFilaParaOTipo() {
        Loan loan = activeLoan(TODAY.plusDays(2));
        assertThat(loan.renewalBlockers(TODAY, POLICY, true)).anyMatch(r -> r.contains("fila de espera"));
    }

    @Test
    void deveBloquearRenovacaoQuandoPendenteDeAssinatura() {
        Loan loan = activeLoan(TODAY.plusDays(2));
        loan.setStatus(LoanStatus.PENDENTE_ASSINATURA);
        assertThat(loan.canRenew(TODAY, POLICY, false)).isFalse();
    }

    @Test
    void deveRejeitarRenovacaoQuandoNovaDataNaoPosterior() {
        Loan loan = activeLoan(TODAY.plusDays(2));
        assertThatThrownBy(() -> loan.renew(TODAY.plusDays(2), TODAY, POLICY, false, "op", NOW))
                .isInstanceOf(BusinessException.class);
    }

    // ---------- atraso (RN27) ----------

    @Test
    void deveMarcarAtrasadoQuandoPassouDoVencimento() {
        Loan loan = activeLoan(TODAY.minusDays(1));
        assertThat(loan.markOverdue(TODAY)).isTrue();
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ATRASADO);
        assertThat(loan.daysOverdue(TODAY)).isEqualTo(1);
        assertThat(loan.markOverdue(TODAY)).isFalse();
    }

    @Test
    void deveManterAtivoQuandoVenceHoje() {
        Loan loan = activeLoan(TODAY);
        assertThat(loan.markOverdue(TODAY)).isFalse();
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ATIVO);
        assertThat(loan.isOverdue(TODAY)).isFalse();
        assertThat(loan.isDueSoon(TODAY, 7)).isTrue();
    }

    @Test
    void deveIndicarVencimentoProximoQuandoDentroDoPrazo() {
        assertThat(activeLoan(TODAY.plusDays(7)).isDueSoon(TODAY, 7)).isTrue();
        assertThat(activeLoan(TODAY.plusDays(8)).isDueSoon(TODAY, 7)).isFalse();
        assertThat(activeLoan(TODAY.minusDays(1)).isDueSoon(TODAY, 7)).isFalse();
    }

    // ---------- devolução e cancelamento ----------

    @Test
    void deveDevolverQuandoAtivoOuAtrasado() {
        Loan loan = activeLoan(TODAY.minusDays(2));
        loan.markOverdue(TODAY);
        loan.markReturned(NOW);
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.DEVOLVIDO);
        assertThat(loan.getReturnedAt()).isEqualTo(NOW);
        assertThat(loan.isOpen()).isFalse();
        assertThatThrownBy(() -> loan.markReturned(NOW)).isInstanceOf(BusinessException.class);
    }

    @Test
    void deveCancelarQuandoEmAbertoComMotivo() {
        Loan loan = activeLoan(TODAY.plusDays(5));
        loan.cancel("Registrado por engano", "admin", NOW);
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.CANCELADO);
        assertThat(loan.getCancelReason()).isEqualTo("Registrado por engano");
        assertThat(loan.getCanceledBy()).isEqualTo("admin");
        assertThat(loan.getCanceledAt()).isEqualTo(NOW);
    }

    @Test
    void deveRejeitarCancelamentoQuandoSemMotivoOuJaEncerrado() {
        Loan loan = activeLoan(TODAY.plusDays(5));
        assertThatThrownBy(() -> loan.cancel(" ", "admin", NOW)).isInstanceOf(BusinessException.class);
        loan.markReturned(NOW);
        assertThatThrownBy(() -> loan.cancel("motivo", "admin", NOW)).isInstanceOf(BusinessException.class);
    }

    @Test
    void deveIdentificarStatusEmAbertoQuandoPendenteAtivoOuAtrasado() {
        assertThat(LoanStatus.openStatuses())
                .containsExactlyInAnyOrder(LoanStatus.PENDENTE_ASSINATURA, LoanStatus.ATIVO, LoanStatus.ATRASADO);
        assertThat(LoanStatus.DEVOLVIDO.isOpen()).isFalse();
        assertThat(LoanStatus.CANCELADO.isOpen()).isFalse();
    }
}
