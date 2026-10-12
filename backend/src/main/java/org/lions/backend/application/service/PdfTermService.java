package org.lions.backend.application.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.lions.backend.domain.entity.Loan;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

@Slf4j
@Service
public class PdfTermService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public record GeneratedTerm(byte[] pdfBytes, String sha256Hash) {}

    public GeneratedTerm generateResponsibilityTerm(Loan loan) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font clauseFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9);

            Paragraph title = new Paragraph("LIONS CLUBE DE MEDIANEIRA - DISTRITO LD-1", titleFont);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(title);

            Paragraph subTitle = new Paragraph("TERMO DE RESPONSABILIDADE E EMPRÉSTIMO DE EQUIPAMENTO ORTOPÉDICO", subTitleFont);
            subTitle.setAlignment(Paragraph.ALIGN_CENTER);
            subTitle.setSpacingAfter(15);
            document.add(subTitle);

            String beneficiaryName = loan.getOrder().getRequester().getBeneficiaryName();
            String doc = loan.getOrder().getRequester().getDocumentNumber();
            String equipment = loan.getEquipment().getType().getDescription();
            String patrimony = loan.getEquipment().getPatrimonyNumber();
            String initialDate = loan.getLoanDate().format(DATE_FORMATTER);
            String dueDate = loan.getCurrentDueDate().format(DATE_FORMATTER);

            String bodyText = String.format(
                    "Pelo presente instrumento, o(a) Beneficiário(a) / Responsável Legal %s, inscrito(a) no documento nº %s, " +
                    "declara receber em regime de empréstimo gratuito e temporário do LIONS CLUBE DE MEDIANEIRA o equipamento abaixo especificado:\n\n" +
                    "• Equipamento: %s\n" +
                    "• Número de Patrimônio: %s\n" +
                    "• Condição de Conservação na Entrega: %s\n" +
                    "• Data de Início: %s\n" +
                    "• Prazo de Vencimento Inicial (90 dias): %s\n\n" +
                    "CLÁUSULAS E COMPROMISSOS:\n" +
                    "1. O solicitante compromete-se a zelar pelo bom estado de conservação do bem recebido.\n" +
                    "2. A cada 90 dias, o clube entrará em contato para verificar a continuidade do uso. O empréstimo poderá ser renovado sucessivamente enquanto perdurar a necessidade comprovada.\n" +
                    "3. O solicitante obriga-se a comunicar imediatamente ao Lions Clube caso não necessite mais do equipamento, permitindo a pronta devolução e o atendimento a outras pessoas da comunidade.\n" +
                    "4. Em caso de avaria provocada por mau uso ou perda, o beneficiário/responsável compromete-se a arcar com os custos de reparo ou reposição do bem.\n\n" +
                    "Termo gerado pelo sistema OrtoEmpresta em %s.",
                    beneficiaryName, doc, equipment, patrimony, loan.getInitialCondition().getDescription(),
                    initialDate, dueDate, java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
            );

            Paragraph body = new Paragraph(bodyText, bodyFont);
            body.setSpacingAfter(20);
            document.add(body);

            Paragraph acceptance = new Paragraph(
                    "Aceite Eletrônico Formalizado:\n" +
                    "Identificação: " + (loan.getAcceptedByName() != null ? loan.getAcceptedByName() : beneficiaryName) + "\n" +
                    "Data e Hora do Registro: " + (loan.getElectronicAcceptanceDate() != null ? loan.getElectronicAcceptanceDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "Pendente de confirmação na entrega"),
                    clauseFont
            );
            document.add(acceptance);

            document.close();

            byte[] pdfBytes = out.toByteArray();
            String sha256 = calculateSha256(pdfBytes);

            return new GeneratedTerm(pdfBytes, sha256);
        } catch (Exception e) {
            log.error("Erro ao gerar termo em PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Falha na geração do Termo de Responsabilidade em PDF: " + e.getMessage());
        }
    }

    private String calculateSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "HASH_ERROR";
        }
    }
}
