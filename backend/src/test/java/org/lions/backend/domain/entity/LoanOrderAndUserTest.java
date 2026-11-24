package org.lions.backend.domain.entity;

import org.junit.jupiter.api.Test;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.OrderStatus;
import org.lions.backend.domain.enums.UserRole;
import org.lions.backend.domain.exception.BusinessException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanOrderAndUserTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2025, 3, 10, 11, 0);

    private static LoanOrder pending() {
        return LoanOrder.builder()
                .id(1L)
                .beneficiary(Beneficiary.builder().id(2L).build())
                .equipmentType(EquipmentType.MULETAS)
                .build();
    }

    // ---------- LoanOrder ----------

    @Test
    void deveIniciarPendenteParaSiQuandoCriada() {
        LoanOrder order = pending();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDENTE_ANALISE);
        assertThat(order.isForSelf()).isTrue();
        assertThat(order.isPending()).isTrue();
        order.validate();
    }

    @Test
    void deveExigirNomeDoSolicitanteQuandoPedidoParaTerceiro() {
        LoanOrder order = pending();
        order.setForSelf(false);
        assertThatThrownBy(order::validate).isInstanceOf(BusinessException.class);
        order.setRequesterName("Filha");
        order.validate();
    }

    @Test
    void deveRejeitarQuandoSemBeneficiarioOuTipo() {
        assertThatThrownBy(() -> LoanOrder.builder().equipmentType(EquipmentType.MULETAS).build().validate())
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> LoanOrder.builder().beneficiary(new Beneficiary()).build().validate())
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deveAprovarQuandoPendenteOuEmFila() {
        LoanOrder order = pending();
        order.approve("op", NOW);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.APROVADO);
        assertThat(order.getDecidedBy()).isEqualTo("op");
        assertThat(order.getDecidedAt()).isEqualTo(NOW);

        LoanOrder queued = pending();
        queued.sendToWaitlist("op", NOW);
        assertThat(queued.getStatus()).isEqualTo(OrderStatus.EM_FILA_ESPERA);
        queued.approve("op", NOW);
        assertThat(queued.getStatus()).isEqualTo(OrderStatus.APROVADO);
    }

    @Test
    void deveRejeitarComMotivoQuandoPendente() {
        LoanOrder order = pending();
        assertThatThrownBy(() -> order.reject(" ", "op", NOW)).isInstanceOf(BusinessException.class);
        order.reject("Fora da área de atendimento", "op", NOW);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REJEITADO);
        assertThat(order.getRejectionReason()).isEqualTo("Fora da área de atendimento");
    }

    @Test
    void deveBloquearTransicaoQuandoSolicitacaoJaDecidida() {
        LoanOrder order = pending();
        order.approve("op", NOW);
        assertThatThrownBy(() -> order.reject("x", "op", NOW)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> order.approve("op", NOW)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> order.sendToWaitlist("op", NOW)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> order.cancel("x", "op", NOW)).isInstanceOf(BusinessException.class);
    }

    @Test
    void deveCancelarComMotivoQuandoEmFila() {
        LoanOrder order = pending();
        order.sendToWaitlist("op", NOW);
        assertThatThrownBy(() -> order.reject("x", "op", NOW)).isInstanceOf(BusinessException.class);
        order.cancel("Removido da fila", "op", NOW);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELADO);
        assertThat(order.getRejectionReason()).isEqualTo("Removido da fila");
    }

    // ---------- User (RN02) ----------

    @Test
    void deveAlterarPerfilQuandoOutroUsuario() {
        User user = User.builder().username("ana").role(UserRole.ROLE_VIEWER).active(true).build();
        user.changeRole(UserRole.ROLE_OPERATOR, "marcio");
        assertThat(user.getRole()).isEqualTo(UserRole.ROLE_OPERATOR);
        assertThat(user.isAdmin()).isFalse();
    }

    @Test
    void deveBloquearAlteracaoDoProprioPerfilQuandoMesmoUsuario() {
        User user = User.builder().username("marcio").role(UserRole.ROLE_ADMIN).active(true).build();
        assertThatThrownBy(() -> user.changeRole(UserRole.ROLE_VIEWER, "MARCIO"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("próprio perfil");
        assertThat(user.getRole()).isEqualTo(UserRole.ROLE_ADMIN);
        assertThatThrownBy(() -> user.changeRole(null, "outro")).isInstanceOf(BusinessException.class);
    }

    @Test
    void deveAtivarDesativarERegistrarLoginQuandoSolicitado() {
        User user = User.builder().username("ana").role(UserRole.ROLE_ADMIN).active(true).build();
        assertThat(user.isAdmin()).isTrue();
        user.deactivate();
        assertThat(user.isActive()).isFalse();
        user.activate();
        assertThat(user.isActive()).isTrue();
        user.registerLogin(NOW);
        assertThat(user.getLastLoginAt()).isEqualTo(NOW);
    }
}
