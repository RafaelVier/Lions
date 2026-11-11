package org.lions.backend.domain.entity;

import org.junit.jupiter.api.Test;
import org.lions.backend.domain.enums.DocumentType;
import org.lions.backend.domain.enums.NotificationChannel;
import org.lions.backend.domain.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BeneficiaryTest {

    private static final LocalDate TODAY = LocalDate.of(2025, 3, 10);
    private static final LocalDateTime NOW = TODAY.atTime(9, 30);

    private static Beneficiary valid() {
        Beneficiary b = Beneficiary.builder()
                .id(12L)
                .fullName("Maria Aparecida da Silva")
                .documentNumber("529.982.247-25")
                .birthDate(LocalDate.of(1950, 5, 20))
                .phone("45999998888")
                .cep("85884-000")
                .street("Rua Paraná")
                .number("100")
                .neighborhood("Centro")
                .city("Medianeira")
                .state("PR")
                .alternativeContactName("João da Silva")
                .alternativeContactPhone("45988887777")
                .build();
        b.grantConsent(Set.of(NotificationChannel.WHATSAPP), "Maria", NOW);
        return b;
    }

    @Test
    void deveAceitarCadastroQuandoCompletoENormalizarCpf() {
        Beneficiary b = valid();
        b.validate(TODAY);
        assertThat(b.getDocumentNumber()).isEqualTo("52998224725");
        assertThat(b.getDocumentType()).isEqualTo(DocumentType.CPF);
        assertThat(b.isActive()).isTrue();
    }

    @Test
    void deveListarObrigatoriosQuandoCamposDaRN05Ausentes() {
        Beneficiary b = valid();
        b.setPhone(null);
        b.setAlternativeContactName(" ");
        b.setBirthDate(null);
        assertThatThrownBy(() -> b.validate(TODAY))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("telefone")
                .hasMessageContaining("nome do contato alternativo")
                .hasMessageContaining("data de nascimento");
    }

    @Test
    void deveRejeitarQuandoCpfInvalido() {
        Beneficiary b = valid();
        b.setDocumentNumber("123.456.789-00");
        assertThatThrownBy(() -> b.validate(TODAY))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CPF inválido");
    }

    @Test
    void deveAceitarDocumentoEstrangeiroSemValidarCpf() {
        Beneficiary b = valid();
        b.setDocumentType(DocumentType.ESTRANGEIRO);
        b.setDocumentNumber("RNE-V123456");
        b.validate(TODAY);
        assertThat(b.getDocumentNumber()).isEqualTo("RNE-V123456");
    }

    @Test
    void deveRejeitarQuandoNascimentoFuturo() {
        Beneficiary b = valid();
        b.setBirthDate(TODAY.plusDays(1));
        assertThatThrownBy(() -> b.validate(TODAY)).isInstanceOf(BusinessException.class);
    }

    @Test
    void deveExigirResponsavelLegalQuandoMenorDeIdade() {
        Beneficiary b = valid();
        b.setBirthDate(TODAY.minusYears(17));
        assertThat(b.isMinor(TODAY)).isTrue();
        assertThatThrownBy(() -> b.validate(TODAY))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("responsável legal");

        b.setLegalGuardianName("Ana Souza");
        b.setLegalGuardianDocument("11144477735");
        b.validate(TODAY);
        assertThat(b.signatoryName(TODAY)).isEqualTo("Ana Souza");
        assertThat(b.signatoryDocument(TODAY)).isEqualTo("11144477735");
    }

    @Test
    void deveConsiderarAdultoQuandoCompleta18AnosHoje() {
        Beneficiary b = valid();
        b.setBirthDate(TODAY.minusYears(18));
        assertThat(b.isMinor(TODAY)).isFalse();
        assertThat(b.signatoryName(TODAY)).isEqualTo("Maria Aparecida da Silva");
    }

    @Test
    void deveExigirResponsavelLegalQuandoIncapaz() {
        Beneficiary b = valid();
        b.setLegallyIncapable(true);
        assertThat(b.requiresLegalGuardian(TODAY)).isTrue();
        assertThatThrownBy(() -> b.validate(TODAY)).isInstanceOf(BusinessException.class);
    }

    @Test
    void deveRejeitarCadastroQuandoSemConsentimento() {
        Beneficiary b = valid();
        b.setConsentGranted(false);
        assertThatThrownBy(() -> b.validate(TODAY))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("consentimento");
    }

    @Test
    void deveRegistrarConsentimentoComCanaisQuandoConcedido() {
        Beneficiary b = valid();
        assertThat(b.isConsentGranted()).isTrue();
        assertThat(b.getConsentTimestamp()).isEqualTo(NOW);
        assertThat(b.getConsentGivenBy()).isEqualTo("Maria");
        assertThat(b.getNotificationChannels()).containsExactly(NotificationChannel.WHATSAPP);

        b.grantConsent(null, "Maria", NOW);
        assertThat(b.getNotificationChannels()).isEmpty();
    }

    @Test
    void deveMascararDocumentoQuandoExibido() {
        Beneficiary b = valid();
        b.validate(TODAY);
        assertThat(b.getMaskedDocument()).isEqualTo("***.982.247-**");
    }

    @Test
    void deveInativarEReativarQuandoComMotivo() {
        Beneficiary b = valid();
        assertThatThrownBy(() -> b.inactivate(" ", NOW)).isInstanceOf(BusinessException.class);
        b.inactivate("Faleceu", NOW);
        assertThat(b.isActive()).isFalse();
        assertThat(b.getInactivationReason()).isEqualTo("Faleceu");
        assertThatThrownBy(() -> b.inactivate("de novo", NOW)).isInstanceOf(BusinessException.class);

        b.reactivate(NOW);
        assertThat(b.isActive()).isTrue();
        assertThat(b.getInactivationReason()).isNull();
    }

    @Test
    void deveAnonimizarDadosPessoaisQuandoSolicitado() {
        Beneficiary b = valid();
        b.anonymize(NOW);
        assertThat(b.isAnonymized()).isTrue();
        assertThat(b.isActive()).isFalse();
        assertThat(b.getFullName()).isEqualTo("Beneficiário anonimizado 12");
        assertThat(b.getDocumentNumber()).isEqualTo("ANON-12");
        assertThat(b.getPhone()).isNull();
        assertThat(b.getStreet()).isNull();
        assertThat(b.getBirthDate()).isNull();
        assertThatThrownBy(() -> b.reactivate(NOW)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> b.anonymize(NOW)).isInstanceOf(BusinessException.class);
    }
}
