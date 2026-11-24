package org.lions.backend.application.service.report;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * CSV para Excel em português: UTF-8 com BOM, separador ponto e vírgula, campos com aspas quando necessário.
 */
public final class CsvReportWriter {

    public static final String CONTENT_TYPE = "text/csv;charset=UTF-8";
    private static final char SEPARATOR = ';';

    private CsvReportWriter() {
    }

    public static byte[] write(ReportTable table) {
        StringBuilder sb = new StringBuilder("﻿");
        line(sb, table.headers());
        table.rows().forEach(row -> line(sb, row));
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void line(StringBuilder sb, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(SEPARATOR);
            }
            sb.append(escape(values.get(i)));
        }
        sb.append("\r\n");
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        // evita injeção de fórmula ao abrir no Excel (=, +, -, @ no início)
        String safe = !value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0 && !value.matches("-?\\d+([.,]\\d+)?")
                ? "'" + value : value;
        if (safe.indexOf(SEPARATOR) >= 0 || safe.indexOf('"') >= 0 || safe.indexOf('\n') >= 0
                || safe.indexOf('\r') >= 0) {
            return '"' + safe.replace("\"", "\"\"") + '"';
        }
        return safe;
    }
}
