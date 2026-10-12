package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lions.backend.domain.enums.DocumentType;
import org.lions.backend.domain.exception.BusinessException;

import java.time.LocalDateTime;

/**
 * Entidade de Domínio (DDD): Solicitante / Beneficiário
 * Trata dados sensíveis com base na LGPD (Art. 5º, II e Art. 11, I).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Requester {

    private Long id;
    private boolean isForSelf; // "Para mim" vs "Outra pessoa"
    private String requesterName; // Nome de quem faz o pedido
    private String relationship; // Ex.: "Filho", "Assistente Social", "Responsável Legal"
    private String beneficiaryName; // Nome de quem vai usar o item
    private DocumentType documentType;
    private String documentNumber; // CPF, CNPJ ou Documento Estrangeiro
    private String whatsapp; // Telefone com DDD
    private String cep;
    private String street;
    private String number;
    private String neighborhood;
    private String city;
    private String state;
    private String complement;
    private String needReason; // Dado sensível de saúde (ex: pós-cirurgia no quadril)
    private boolean consentGranted; // Aceite explícito da política de privacidade e uso de dados de saúde
    private LocalDateTime consentTimestamp;
    private boolean assistedRegistration; // RF02: Feito pelo Márcio/administrador
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void validate() {
        if (beneficiaryName == null || beneficiaryName.isBlank()) {
            throw new BusinessException("O nome de quem vai usar o equipamento é obrigatório.");
        }
        if (!isForSelf && (requesterName == null || requesterName.isBlank())) {
            throw new BusinessException("Quando o pedido é feito por terceiro, o nome do solicitante é obrigatório.");
        }
        if (documentNumber == null || documentNumber.isBlank()) {
            throw new BusinessException("O documento de identificação é obrigatório.");
        }
        if (whatsapp == null || whatsapp.isBlank()) {
            throw new BusinessException("O WhatsApp para contato é obrigatório.");
        }
        if (!consentGranted) {
            throw new BusinessException("É obrigatório autorizar o Lions Clube a tratar os dados para a finalidade do empréstimo (LGPD).");
        }
    }

    /**
     * Retorna o documento mascarado para proteção de privacidade nas listagens (LGPD)
     */
    public String getMaskedDocument() {
        if (documentNumber == null || documentNumber.length() < 6) {
            return "***";
        }
        String clean = documentNumber.replaceAll("\\D", "");
        if (clean.length() == 11) { // CPF
            return String.format("***.%s.%s-**", clean.substring(3, 6), clean.substring(6, 9));
        } else if (clean.length() == 14) { // CNPJ
            return String.format("**.%s.%s/****-**", clean.substring(2, 5), clean.substring(5, 8));
        }
        return "***" + documentNumber.substring(documentNumber.length() - 4);
    }
}
