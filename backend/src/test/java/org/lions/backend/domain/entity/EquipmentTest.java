package org.lions.backend.domain.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.WriteOffReason;
import org.lions.backend.domain.exception.BusinessException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EquipmentTest {

    private static final LocalDate TODAY = LocalDate.of(2025, 3, 10);

    private static Equipment equipment(EquipmentStatus status) {
        return Equipment.builder()
                .id(1L)
                .patrimonyNumber("PT-0001")
                .type(EquipmentType.CADEIRA_DE_RODAS)
                .condition(EquipmentCondition.BOM)
                .status(status)
                .build();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "DISPONIVEL, RESERVADO", "DISPONIVEL, EMPRESTADO", "DISPONIVEL, EM_MANUTENCAO", "DISPONIVEL, BAIXADO",
            "RESERVADO, EMPRESTADO", "RESERVADO, DISPONIVEL",
            "EMPRESTADO, DISPONIVEL", "EMPRESTADO, EM_MANUTENCAO",
            "EM_MANUTENCAO, DISPONIVEL", "EM_MANUTENCAO, BAIXADO"
    })
    void devePermitirTransicaoQuandoPrevistaNaRN12(EquipmentStatus from, EquipmentStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "DISPONIVEL, DISPONIVEL", "RESERVADO, EM_MANUTENCAO", "RESERVADO, BAIXADO",
            "EMPRESTADO, RESERVADO", "EMPRESTADO, BAIXADO",
            "EM_MANUTENCAO, EMPRESTADO", "EM_MANUTENCAO, RESERVADO",
            "BAIXADO, DISPONIVEL", "BAIXADO, EM_MANUTENCAO", "BAIXADO, EMPRESTADO"
    })
    void deveNegarTransicaoQuandoNaoPrevistaNaRN12(EquipmentStatus from, EquipmentStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @Test
    void deveNegarTransicaoQuandoDestinoNulo() {
        assertThat(EquipmentStatus.DISPONIVEL.canTransitionTo(null)).isFalse();
    }

    @Test
    void deveFormatarPatrimonioQuandoIdInformado() {
        assertThat(Equipment.formatPatrimony(7L)).isEqualTo("PT-0007");
        assertThat(Equipment.formatPatrimony(12345L)).isEqualTo("PT-12345");
        Equipment e = Equipment.builder().id(42L).build();
        e.assignPatrimonyNumber();
        assertThat(e.getPatrimonyNumber()).isEqualTo("PT-0042");
    }

    @Test
    void deveRejeitarCadastroQuandoSemTipoOuCondicao() {
        assertThatThrownBy(() -> Equipment.builder().condition(EquipmentCondition.BOM).build().validate())
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> Equipment.builder().type(EquipmentType.ANDADOR).build().validate())
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deveIniciarDisponivelQuandoCriadoPeloBuilder() {
        assertThat(Equipment.builder().build().getStatus()).isEqualTo(EquipmentStatus.DISPONIVEL);
    }

    @Test
    void deveEmprestarQuandoReservadoParaOMesmoBeneficiario() {
        Equipment e = equipment(EquipmentStatus.DISPONIVEL);
        e.reserveFor(10L);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.RESERVADO);
        assertThat(e.getReservedForBeneficiaryId()).isEqualTo(10L);

        e.markAsLoaned(10L);

        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.EMPRESTADO);
        assertThat(e.getReservedForBeneficiaryId()).isNull();
    }

    @Test
    void deveBloquearEmprestimoQuandoReservadoParaOutroBeneficiario() {
        Equipment e = equipment(EquipmentStatus.DISPONIVEL);
        e.reserveFor(10L);

        assertThatThrownBy(() -> e.markAsLoaned(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("reservado para outro beneficiário");
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.RESERVADO);
    }

    @Test
    void deveBloquearEmprestimoQuandoEmManutencao() {
        Equipment e = equipment(EquipmentStatus.EM_MANUTENCAO);
        assertThatThrownBy(() -> e.markAsLoaned(10L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void deveLiberarReservaQuandoReservado() {
        Equipment e = equipment(EquipmentStatus.DISPONIVEL);
        e.reserveFor(10L);
        e.releaseReservation();
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.DISPONIVEL);
        assertThat(e.getReservedForBeneficiaryId()).isNull();
        assertThatThrownBy(e::releaseReservation).isInstanceOf(BusinessException.class);
    }

    @Test
    void deveContarCicloQuandoDevolvido() {
        Equipment e = equipment(EquipmentStatus.EMPRESTADO);
        e.markAsReturned(false);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.DISPONIVEL);
        assertThat(e.getLoanCyclesSinceMaintenance()).isEqualTo(1);

        e.markAsLoaned(5L);
        e.markAsReturned(true);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.EM_MANUTENCAO);
        assertThat(e.getLoanCyclesSinceMaintenance()).isEqualTo(2);
    }

    @Test
    void deveRejeitarDevolucaoQuandoNaoEmprestado() {
        assertThatThrownBy(() -> equipment(EquipmentStatus.DISPONIVEL).markAsReturned(false))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deveZerarCiclosQuandoRetornaDaManutencao() {
        Equipment e = equipment(EquipmentStatus.EM_MANUTENCAO);
        e.setLoanCyclesSinceMaintenance(8);
        e.returnFromMaintenance(TODAY);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.DISPONIVEL);
        assertThat(e.getLoanCyclesSinceMaintenance()).isZero();
        assertThat(e.getLastMaintenanceDate()).isEqualTo(TODAY);
    }

    @Test
    void deveBaixarQuandoDisponivelComMotivo() {
        Equipment e = equipment(EquipmentStatus.DISPONIVEL);
        e.writeOff(WriteOffReason.DOACAO, "Doado à APAE", "marcio", TODAY);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.BAIXADO);
        assertThat(e.isActive()).isFalse();
        assertThat(e.getWriteOffReason()).isEqualTo(WriteOffReason.DOACAO);
        assertThat(e.getWriteOffBy()).isEqualTo("marcio");
        assertThat(e.getWriteOffDate()).isEqualTo(TODAY);
    }

    @Test
    void deveRejeitarBaixaQuandoEmprestadoOuSemMotivo() {
        assertThatThrownBy(() -> equipment(EquipmentStatus.EMPRESTADO)
                .writeOff(WriteOffReason.PERDA, null, "marcio", TODAY))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> equipment(EquipmentStatus.DISPONIVEL).writeOff(null, null, "marcio", TODAY))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deveManterBaixadoComoEstadoFinal() {
        Equipment e = equipment(EquipmentStatus.BAIXADO);
        assertThatThrownBy(e::sendToMaintenance).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> e.reserveFor(1L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> e.markAsLoaned(1L)).isInstanceOf(BusinessException.class);
    }
}
