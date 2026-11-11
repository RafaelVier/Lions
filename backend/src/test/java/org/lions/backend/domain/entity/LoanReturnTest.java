package org.lions.backend.domain.entity;

import org.junit.jupiter.api.Test;
import org.lions.backend.domain.enums.ReturnCondition;
import org.lions.backend.domain.exception.BusinessException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanReturnTest {

    private static final LocalDate TODAY = LocalDate.of(2025, 3, 10);

    @Test
    void deveRegistrarDevolucaoQuandoBomEstado() {
        LoanReturn r = LoanReturn.register(1L, TODAY, ReturnCondition.BOM_ESTADO, null, null, "op");
        assertThat(r.isHasDamage()).isFalse();
        assertThat(r.needsMaintenance()).isFalse();
        assertThat(r.getReceivedBy()).isEqualTo("op");
    }

    @Test
    void deveExigirDescricaoQuandoDanificado() {
        assertThatThrownBy(() -> LoanReturn.register(1L, TODAY, ReturnCondition.DANIFICADO, " ", null, "op"))
                .isInstanceOf(BusinessException.class);
        LoanReturn r = LoanReturn.register(1L, TODAY, ReturnCondition.DANIFICADO, "Apoio de pé quebrado", null, "op");
        assertThat(r.isHasDamage()).isTrue();
        assertThat(r.needsMaintenance()).isTrue();
    }

    @Test
    void deveIndicarManutencaoQuandoPrecisaManutencao() {
        assertThat(LoanReturn.register(1L, TODAY, ReturnCondition.PRECISA_MANUTENCAO, null, null, "op")
                .needsMaintenance()).isTrue();
        assertThatThrownBy(() -> LoanReturn.register(1L, TODAY, null, null, null, "op"))
                .isInstanceOf(BusinessException.class);
    }
}
