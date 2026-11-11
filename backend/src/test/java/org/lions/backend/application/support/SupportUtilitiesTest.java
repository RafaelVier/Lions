package org.lions.backend.application.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.lions.backend.domain.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SupportUtilitiesTest {

    // ---------- CpfValidator ----------

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-25", "52998224725", "111.444.777-35"})
    void deveAceitarCpfQuandoDigitosVerificadoresCorretos(String cpf) {
        assertThat(CpfValidator.isValid(cpf)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"123.456.789-00", "111.111.111-11", "5299822472", "529982247251", "abc", ""})
    void deveRejeitarCpfQuandoInvalido(String cpf) {
        assertThat(CpfValidator.isValid(cpf)).isFalse();
    }

    @Test
    void deveNormalizarDocumentoQuandoComPontuacao() {
        assertThat(CpfValidator.normalize("529.982.247-25")).isEqualTo("52998224725");
        assertThat(CpfValidator.normalize(null)).isNull();
        assertThat(CpfValidator.isValid(null)).isFalse();
    }

    // ---------- LgpdMasker ----------

    @Test
    void deveMascararDocumentoQuandoCpfCnpjOuOutro() {
        assertThat(LgpdMasker.maskDocument("123.456.789-09")).isEqualTo("***.456.789-**");
        assertThat(LgpdMasker.maskDocument("12345678000199")).isEqualTo("**.345.678/****-**");
        assertThat(LgpdMasker.maskDocument("RNE-V123456")).isEqualTo("***3456");
        assertThat(LgpdMasker.maskDocument("AB1")).isEqualTo("***");
        assertThat(LgpdMasker.maskDocument(null)).isNull();
    }

    @Test
    void deveMascararNomeQuandoComSobrenomes() {
        assertThat(LgpdMasker.maskName("Maria Aparecida da Silva")).isEqualTo("Maria A. D. S.");
        assertThat(LgpdMasker.maskName("Maria")).isEqualTo("Maria");
    }

    @Test
    void deveMascararTelefoneEEnderecoQuandoExibidos() {
        assertThat(LgpdMasker.maskPhone("(45) 99999-1234")).isEqualTo("(**) *****-1234");
        assertThat(LgpdMasker.maskPhone("12")).isEqualTo("***");
        assertThat(LgpdMasker.maskAddress("Medianeira", "PR")).isEqualTo("*** - Medianeira/PR");
        assertThat(LgpdMasker.maskAddress(null, null)).isEqualTo("***");
    }

    // ---------- PasswordPolicy ----------

    @Test
    void deveAceitarSenhaQuandoForte() {
        assertThat(PasswordPolicy.isStrong("Lions@123")).isTrue();
        PasswordPolicy.validate("Lions@123");
    }

    @Test
    void deveListarViolacoesQuandoSenhaFraca() {
        assertThat(PasswordPolicy.violations("lions123")).hasSize(2);
        assertThat(PasswordPolicy.violations("Ab@1")).anyMatch(v -> v.contains("mínimo 8"));
        assertThat(PasswordPolicy.violations("Lions @123")).anyMatch(v -> v.contains("espaços"));
        assertThat(PasswordPolicy.violations(null)).hasSize(5);
        assertThatThrownBy(() -> PasswordPolicy.validate("12345678")).isInstanceOf(BusinessException.class);
    }

    // ---------- WhatsAppMessageHelper ----------

    private final WhatsAppMessageHelper whatsApp = new WhatsAppMessageHelper();

    @Test
    void deveGerarLinkComDdiQuandoTelefoneNacional() {
        assertThat(whatsApp.sanitizePhone("(45) 99999-1234")).isEqualTo("5545999991234");
        assertThat(whatsApp.sanitizePhone("5545999991234")).isEqualTo("5545999991234");
        assertThat(whatsApp.buildLink("(45) 99999-1234", "Olá Maria"))
                .isEqualTo("https://wa.me/5545999991234?text=Ol%C3%A1%20Maria");
        assertThat(whatsApp.buildLink("45999991234", null)).isEqualTo("https://wa.me/5545999991234");
        assertThatThrownBy(() -> whatsApp.buildLink(" ", "x")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveCitarDataQuandoGerarMensagensPadrao() {
        String reminder = whatsApp.dueDateReminderMessage("Maria", "Andador", "PT-0001", LocalDate.of(2025, 3, 17));
        String overdue = whatsApp.overdueMessage("Maria", "Andador", null, LocalDate.of(2025, 3, 9));
        String reserved = whatsApp.reservationAvailableMessage("Maria", "Andador", "PT-0001",
                LocalDateTime.of(2025, 3, 12, 14, 30));

        assertThat(reminder).contains("Maria", "Andador (PT-0001)", "17/03/2025").doesNotContain("90 dias");
        assertThat(overdue).contains("venceu em 09/03/2025").doesNotContain("(null)");
        assertThat(reserved).contains("12/03/2025 às 14:30");
    }
}
