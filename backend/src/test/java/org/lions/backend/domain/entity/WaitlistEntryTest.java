package org.lions.backend.domain.entity;

import org.junit.jupiter.api.Test;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.PriorityReason;
import org.lions.backend.domain.enums.WaitlistStatus;
import org.lions.backend.domain.exception.BusinessException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WaitlistEntryTest {

    private static final LocalDateTime QUEUED = LocalDateTime.of(2025, 3, 1, 9, 0);
    private static final LocalDateTime NOW = LocalDateTime.of(2025, 3, 10, 10, 0);

    private static WaitlistEntry waiting() {
        return WaitlistEntry.builder()
                .id(1L)
                .beneficiary(Beneficiary.builder().id(5L).build())
                .equipmentType(EquipmentType.CADEIRA_DE_RODAS)
                .queuedAt(QUEUED)
                .originalRequestedAt(QUEUED)
                .build();
    }

    @Test
    void deveIniciarAguardandoQuandoCriada() {
        WaitlistEntry entry = waiting();
        assertThat(entry.getStatus()).isEqualTo(WaitlistStatus.AGUARDANDO);
        assertThat(entry.isActive()).isTrue();
    }

    @Test
    void deveReservarPorPrazoQuandoAguardando() {
        WaitlistEntry entry = waiting();
        entry.reserve(77L, NOW, 48);

        assertThat(entry.getStatus()).isEqualTo(WaitlistStatus.RESERVADO);
        assertThat(entry.getReservedEquipmentId()).isEqualTo(77L);
        assertThat(entry.getReservedAt()).isEqualTo(NOW);
        assertThat(entry.getReservationExpiresAt()).isEqualTo(NOW.plusHours(48));
        assertThat(entry.isActive()).isTrue();
        assertThat(entry.isReservationExpired(NOW.plusHours(48))).isFalse();
        assertThat(entry.isReservationExpired(NOW.plusHours(48).plusMinutes(1))).isTrue();
    }

    @Test
    void deveRejeitarReservaQuandoJaReservadoOuSemEquipamento() {
        WaitlistEntry entry = waiting();
        assertThatThrownBy(() -> entry.reserve(null, NOW, 48)).isInstanceOf(BusinessException.class);
        entry.reserve(77L, NOW, 48);
        assertThatThrownBy(() -> entry.reserve(78L, NOW, 48)).isInstanceOf(BusinessException.class);
    }

    @Test
    void deveVoltarAoFimDaFilaQuandoPrimeiraExpiracao() {
        WaitlistEntry entry = waiting();
        entry.reserve(77L, NOW, 48);
        LocalDateTime expiredAt = NOW.plusHours(49);

        boolean removed = entry.expireOrDecline(expiredAt, 2);

        assertThat(removed).isFalse();
        assertThat(entry.getStatus()).isEqualTo(WaitlistStatus.AGUARDANDO);
        assertThat(entry.getQueuedAt()).isEqualTo(expiredAt);
        assertThat(entry.getOriginalRequestedAt()).isEqualTo(QUEUED);
        assertThat(entry.getConsecutiveExpirations()).isEqualTo(1);
        assertThat(entry.getReservedEquipmentId()).isNull();
        assertThat(entry.getReservationExpiresAt()).isNull();
    }

    @Test
    void deveRemoverDaFilaQuandoSegundaExpiracaoConsecutiva() {
        WaitlistEntry entry = waiting();
        entry.reserve(77L, NOW, 48);
        entry.expireOrDecline(NOW.plusHours(49), 2);
        entry.reserve(78L, NOW.plusDays(3), 48);

        boolean removed = entry.expireOrDecline(NOW.plusDays(5), 2);

        assertThat(removed).isTrue();
        assertThat(entry.getStatus()).isEqualTo(WaitlistStatus.REMOVIDO);
        assertThat(entry.getRemovedReason()).isEqualTo(WaitlistEntry.REMOVED_BY_EXPIRATIONS);
        assertThat(entry.getClosedAt()).isEqualTo(NOW.plusDays(5));
        assertThat(entry.isActive()).isFalse();
    }

    @Test
    void deveRejeitarExpiracaoQuandoSemReserva() {
        assertThatThrownBy(() -> waiting().expireOrDecline(NOW, 2)).isInstanceOf(BusinessException.class);
    }

    @Test
    void deveAtenderQuandoAtiva() {
        WaitlistEntry entry = waiting();
        entry.reserve(77L, NOW, 48);
        entry.serve(NOW.plusHours(2));
        assertThat(entry.getStatus()).isEqualTo(WaitlistStatus.ATENDIDO);
        assertThat(entry.getClosedAt()).isEqualTo(NOW.plusHours(2));
        assertThatThrownBy(() -> entry.serve(NOW)).isInstanceOf(BusinessException.class);
    }

    @Test
    void deveRemoverComMotivoQuandoAtiva() {
        WaitlistEntry entry = waiting();
        assertThatThrownBy(() -> entry.remove(" ", NOW)).isInstanceOf(BusinessException.class);
        entry.remove("Beneficiário inativado", NOW);
        assertThat(entry.getStatus()).isEqualTo(WaitlistStatus.REMOVIDO);
        assertThat(entry.getRemovedReason()).isEqualTo("Beneficiário inativado");
        assertThatThrownBy(() -> entry.remove("de novo", NOW)).isInstanceOf(BusinessException.class);
    }

    @Test
    void deveDefinirPrioridadeQuandoMotivoEJustificativaInformados() {
        WaitlistEntry entry = waiting();
        entry.setPriority(PriorityReason.POS_OPERATORIO, "Cirurgia de quadril", "admin");
        assertThat(entry.isPriority()).isTrue();
        assertThat(entry.getPriorityReason()).isEqualTo(PriorityReason.POS_OPERATORIO);
        assertThat(entry.getPriorityAuthorizedBy()).isEqualTo("admin");

        entry.clearPriority();
        assertThat(entry.isPriority()).isFalse();
        assertThat(entry.getPriorityReason()).isNull();
    }

    @Test
    void deveRejeitarPrioridadeQuandoSemJustificativaOuInativa() {
        WaitlistEntry entry = waiting();
        assertThatThrownBy(() -> entry.setPriority(PriorityReason.PESSOA_IDOSA, " ", "admin"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> entry.setPriority(null, "x", "admin"))
                .isInstanceOf(BusinessException.class);
        entry.remove("desistiu", NOW);
        assertThatThrownBy(() -> entry.setPriority(PriorityReason.PESSOA_IDOSA, "x", "admin"))
                .isInstanceOf(BusinessException.class);
    }
}
