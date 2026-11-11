package org.lions.backend.application.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lions.backend.domain.entity.Beneficiary;
import org.lions.backend.domain.entity.Loan;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

/**
 * Geração do termo de responsabilidade em PDF + hash SHA-256 (mínimo da Onda 0).
 * Frente D completa o fluxo (armazenamento via {@code FileStoragePort}, RN21/RN61).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfTermService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final Clock clock;

    public record GeneratedTerm(byte[] pdfBytes, String sha256Hash) {
    }

    public GeneratedTerm generateResponsibilityTerm(Loan loan) {
        LocalDate today = LocalDate.now(clock);
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

            Paragraph subTitle = new Paragraph(
                    "TERMO DE RESPONSABILIDADE E EMPRÉSTIMO DE EQUIPAMENTO ORTOPÉDICO", subTitleFont);
            subTitle.setAlignment(Paragraph.ALIGN_CENTER);
            subTitle.setSpacingAfter(15);
            document.add(subTitle);

            Beneficiary beneficiary = loan.getBeneficiary();
            String signatoryName = beneficiary.signatoryName(today);
            String signatoryDocument = beneficiary.signatoryDocument(today);
            String equipment = loan.getEquipment().getType().getDescription();
            String patrimony = loan.getEquipment().getPatrimonyNumber();
            String condition = loan.getInitialCondition() != null
                    ? loan.getInitialCondition().getDescription()
                    : loan.getEquipment().getCondition().getDescription();

            String bodyText = String.format(
                    "Pelo presente instrumento, %s, documento nº %s, %s"
                            + "declara receber em regime de empréstimo gratuito e temporário do LIONS CLUBE DE MEDIANEIRA "
                            + "o equipamento abaixo especificado:\n\n"
                            + "• Equipamento: %s\n"
                            + "• Número de Patrimônio: %s\n"
                            + "• Condição de Conservação na Entrega: %s\n"
                            + "• Data de Início: %s\n"
                            + "• Data Prevista de Devolução: %s\n\n"
                            + "CLÁUSULAS E COMPROMISSOS:\n"
                            + "1. O responsável compromete-se a zelar pelo bom estado de conservação do bem recebido.\n"
                            + "2. O equipamento deve ser devolvido até a data prevista, salvo renovação registrada pelo clube.\n"
                            + "3. O responsável obriga-se a comunicar imediatamente ao Lions Clube caso não necessite mais do "
                            + "equipamento, permitindo a pronta devolução e o atendimento a outras pessoas da comunidade.\n"
                            + "4. Em caso de avaria provocada por mau uso ou perda, o responsável compromete-se a arcar com os "
                            + "custos de reparo ou reposição do bem.\n\n"
                            + "Termo gerado pelo sistema em %s.",
                    signatoryName,
                    signatoryDocument,
                    beneficiary.requiresLegalGuardian(today)
                            ? "na qualidade de responsável legal por " + beneficiary.getFullName() + ", "
                            : "",
                    equipment, patrimony, condition,
                    loan.getLoanDate().format(DATE_FORMATTER),
                    loan.getExpectedReturnDate().format(DATE_FORMATTER),
                    LocalDateTime.now(clock).format(DATE_TIME_FORMATTER));

            Paragraph body = new Paragraph(bodyText, bodyFont);
            body.setSpacingAfter(20);
            document.add(body);

            Paragraph signature = new Paragraph(
                    "Assinatura:\n"
                            + "Identificação: " + (loan.getSignedByName() != null ? loan.getSignedByName() : signatoryName) + "\n"
                            + "Data e Hora do Registro: " + (loan.getSignedAt() != null
                            ? loan.getSignedAt().format(DATE_TIME_FORMATTER)
                            : "Pendente de assinatura"),
                    clauseFont);
            document.add(signature);

            document.close();

            byte[] pdfBytes = out.toByteArray();
            return new GeneratedTerm(pdfBytes, sha256(pdfBytes));
        } catch (Exception e) {
            log.error("Erro ao gerar termo em PDF: {}", e.getMessage(), e);
            throw new IllegalStateException("Falha na geração do Termo de Responsabilidade em PDF.", e);
        }
    }

    public static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
