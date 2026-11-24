package org.lions.backend.application.service.report;

import org.junit.jupiter.api.Test;
import org.lions.backend.application.service.dashboard.DashboardServiceTestAccess;
import org.lions.backend.domain.exception.BusinessException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportSupportTest {

    private static final LocalDate TODAY = LocalDate.of(2025, 3, 10);

    @Test
    void deveUsarUltimos30DiasQuandoPeriodoNaoInformado() {
        assertThat(ReportPeriod.resolve(null, null, TODAY, 30))
                .isEqualTo(new ReportPeriod(LocalDate.of(2025, 2, 9), TODAY));
        assertThat(ReportPeriod.resolve(null, LocalDate.of(2025, 1, 31), TODAY, 30).start())
                .isEqualTo(LocalDate.of(2025, 1, 2));
        assertThat(ReportPeriod.resolve(LocalDate.of(2025, 3, 1), null, TODAY, 30).end()).isEqualTo(TODAY);
        assertThatThrownBy(() -> ReportPeriod.resolve(TODAY, TODAY.minusDays(1), TODAY, 30))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deveEscaparCsvEEvitarInjecaoDeFormula() {
        assertThat(CsvReportWriter.escape("simples")).isEqualTo("simples");
        assertThat(CsvReportWriter.escape("a;b")).isEqualTo("\"a;b\"");
        assertThat(CsvReportWriter.escape("diz \"oi\"")).isEqualTo("\"diz \"\"oi\"\"\"");
        assertThat(CsvReportWriter.escape("=SOMA(A1)")).isEqualTo("'=SOMA(A1)");
        assertThat(CsvReportWriter.escape("-12,50")).isEqualTo("-12,50");
        assertThat(CsvReportWriter.escape(null)).isEmpty();

        byte[] csv = CsvReportWriter.write(new ReportTable("t", List.of(), List.of("A", "B"),
                List.of(List.of("1", "x;y")), null));
        assertThat(new String(csv, StandardCharsets.UTF_8)).isEqualTo("﻿A;B\r\n1;\"x;y\"\r\n");
    }

    @Test
    void deveGerarPdfQuandoTabelaVazia() {
        byte[] pdf = PdfReportWriter.write(new ReportTable("Relatório de teste", List.of("Período: x"),
                List.of("A", "B"), List.of(), "Nota"), "Gerado em hoje");
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void deveClassificarFaixaEtaria() {
        assertThat(ReportService.ageRange(null)).isNull();
        assertThat(ReportService.ageRange(10)).isEqualTo("0–17");
        assertThat(ReportService.ageRange(18)).isEqualTo("18–59");
        assertThat(ReportService.ageRange(60)).isEqualTo("60–79");
        assertThat(ReportService.ageRange(85)).isEqualTo("80+");
    }

    @Test
    void deveCalcularTaxaDeOcupacaoSobreAtivos() {
        assertThat(DashboardServiceTestAccess.rate(1, 3)).isEqualByComparingTo(new BigDecimal("33.3"));
        assertThat(DashboardServiceTestAccess.rate(0, 0)).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
