package org.lions.backend.application.service.report;

import java.util.List;

/** Conteúdo tabular de um relatório, independente do formato (CSV/PDF). */
public record ReportTable(String title, List<String> filters, List<String> headers, List<List<String>> rows,
                          String note) {
}
