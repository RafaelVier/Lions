package org.lions.backend.domain.entity;

import org.junit.jupiter.api.Test;
import org.lions.backend.domain.enums.CostType;
import org.lions.backend.domain.enums.MaintenanceStatus;
import org.lions.backend.domain.enums.MaintenanceType;
import org.lions.backend.domain.enums.MaintenanceVerdict;
import org.lions.backend.domain.exception.BusinessException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaintenanceTest {

    private static final LocalDate TODAY = LocalDate.of(2025, 3, 10);
    private static final LocalDateTime NOW = TODAY.atTime(14, 0);

    private static Maintenance open() {
        Maintenance m = Maintenance.builder()
                .id(3L)
                .equipment(Equipment.builder().id(1L).build())
                .type(MaintenanceType.CORRETIVA)
                .defectDescription("Roda dianteira travando")
                .entryDate(TODAY.minusDays(2))
                .technicalResponsible("Oficina do João")
                .build();
        m.validateOpening();
        return m;
    }

    @Test
    void deveAbrirEmAndamentoQuandoCamposObrigatoriosInformados() {
        Maintenance m = open();
        assertThat(m.getStatus()).isEqualTo(MaintenanceStatus.EM_ANDAMENTO);
        assertThat(m.isOpen()).isTrue();
        assertThat(m.getCosts()).isEmpty();
        assertThat(m.totalCost()).isEqualByComparingTo("0");
    }

    @Test
    void deveRejeitarAberturaQuandoFaltamCamposDaRN36() {
        Maintenance m = Maintenance.builder().type(MaintenanceType.PREVENTIVA).build();
        assertThatThrownBy(m::validateOpening)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("equipamento")
                .hasMessageContaining("descrição do defeito")
                .hasMessageContaining("data de entrada")
                .hasMessageContaining("responsável técnico");
    }

    @Test
    void deveSomarCustosQuandoEmAndamento() {
        Maintenance m = open();
        MaintenanceCost part = m.addCost(CostType.PECA, "Roda", "Roda dianteira", new BigDecimal("80.00"), "op", NOW);
        m.addCost(CostType.SERVICO, "Mão de obra", null, new BigDecimal("40.50"), "op", NOW);

        assertThat(part.getMaintenanceId()).isEqualTo(3L);
        assertThat(part.isAdjustment()).isFalse();
        assertThat(part.getRegisteredAt()).isEqualTo(NOW);
        assertThat(m.getCosts()).hasSize(2);
        assertThat(m.totalCost()).isEqualByComparingTo("120.50");
    }

    @Test
    void deveRejeitarCustoQuandoNegativoOuIncompleto() {
        Maintenance m = open();
        assertThatThrownBy(() -> m.addCost(CostType.PECA, "Roda", null, new BigDecimal("-1"), "op", NOW))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> m.addCost(null, "Roda", null, BigDecimal.ONE, "op", NOW))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> m.addCost(CostType.PECA, " ", null, BigDecimal.ONE, "op", NOW))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> m.addCost(CostType.PECA, "Roda", null, null, "op", NOW))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deveRejeitarCustoQuandoManutencaoEncerrada() {
        Maintenance m = open();
        m.close(MaintenanceVerdict.APTO, "Ok", "op", TODAY);
        assertThatThrownBy(() -> m.addCost(CostType.SERVICO, "Extra", null, BigDecimal.TEN, "op", NOW))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ajuste");
    }

    @Test
    void devePermitirAjusteComMotivoQuandoManutencaoEncerrada() {
        Maintenance m = open();
        m.addCost(CostType.PECA, "Roda", null, new BigDecimal("100"), "op", NOW);
        m.close(MaintenanceVerdict.APTO, "Ok", "op", TODAY);

        assertThatThrownBy(() -> m.addAdjustment(CostType.PECA, "Desconto", new BigDecimal("-20"), " ", "admin", NOW))
                .isInstanceOf(BusinessException.class);

        MaintenanceCost adj = m.addAdjustment(CostType.PECA, "Desconto", new BigDecimal("-20"), "Nota corrigida",
                "admin", NOW);
        assertThat(adj.isAdjustment()).isTrue();
        assertThat(adj.getAdjustmentReason()).isEqualTo("Nota corrigida");
        assertThat(m.totalCost()).isEqualByComparingTo("80");
    }

    @Test
    void deveEncerrarComParecerQuandoEmAndamento() {
        Maintenance apto = open();
        apto.close(MaintenanceVerdict.APTO, "Trocada roda", "op", TODAY);
        assertThat(apto.getStatus()).isEqualTo(MaintenanceStatus.CONCLUIDA_APTO);
        assertThat(apto.getExitDate()).isEqualTo(TODAY);
        assertThat(apto.getClosedBy()).isEqualTo("op");
        assertThat(apto.isOpen()).isFalse();

        Maintenance inservivel = open();
        inservivel.close(MaintenanceVerdict.INSERVIVEL, "Estrutura trincada", "op", TODAY);
        assertThat(inservivel.getStatus()).isEqualTo(MaintenanceStatus.CONCLUIDA_INSERVIVEL);
    }

    @Test
    void deveRejeitarEncerramentoQuandoJaEncerradaSemParecerOuAntesDaEntrada() {
        Maintenance m = open();
        assertThatThrownBy(() -> m.close(null, null, "op", TODAY)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> m.close(MaintenanceVerdict.APTO, null, "op", TODAY.minusDays(5)))
                .isInstanceOf(BusinessException.class);
        m.close(MaintenanceVerdict.APTO, null, "op", TODAY);
        assertThatThrownBy(() -> m.close(MaintenanceVerdict.APTO, null, "op", TODAY))
                .isInstanceOf(BusinessException.class);
    }
}
