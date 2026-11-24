package org.lions.backend.application.service.report;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

/** Relatório em PDF (A4 paisagem, tabela com cabeçalho repetido em cada página). */
public final class PdfReportWriter {

    public static final String CONTENT_TYPE = "application/pdf";

    private PdfReportWriter() {
    }

    public static byte[] write(ReportTable table, String generatedLine) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 24, 24, 28, 28);
        PdfWriter.getInstance(document, out);
        document.open();
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

        document.add(new Paragraph("Banco Ortopédico — Lions Clube de Medianeira", infoFont));
        Paragraph title = new Paragraph(table.title(), titleFont);
        title.setSpacingAfter(4);
        document.add(title);
        for (String filter : table.filters()) {
            document.add(new Paragraph(filter, infoFont));
        }
        document.add(new Paragraph(generatedLine, infoFont));

        PdfPTable pdfTable = new PdfPTable(table.headers().size());
        pdfTable.setWidthPercentage(100);
        pdfTable.setSpacingBefore(8);
        pdfTable.setHeaderRows(1);
        for (String header : table.headers()) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(new Color(0x1F, 0x3A, 0x5F));
            cell.setPadding(4);
            pdfTable.addCell(cell);
        }
        if (table.rows().isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("Nenhum registro para os filtros informados.", cellFont));
            empty.setColspan(table.headers().size());
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            empty.setPadding(6);
            pdfTable.addCell(empty);
        }
        for (var row : table.rows()) {
            for (String value : row) {
                PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, cellFont));
                cell.setPadding(3);
                pdfTable.addCell(cell);
            }
        }
        document.add(pdfTable);
        Paragraph total = new Paragraph(table.rows().size() + " registro(s).", infoFont);
        total.setSpacingBefore(6);
        document.add(total);
        if (table.note() != null) {
            document.add(new Paragraph(table.note(), infoFont));
        }
        document.close();
        return out.toByteArray();
    }
}
