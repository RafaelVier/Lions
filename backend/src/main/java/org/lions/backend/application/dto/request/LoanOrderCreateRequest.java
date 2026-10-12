package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lions.backend.domain.enums.DocumentType;
import org.lions.backend.domain.enums.EquipmentType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanOrderCreateRequest {

    // Etapa 1: Dados do Solicitante / Beneficiário
    private boolean isForSelf; // true = Para mim, false = Outra pessoa
    private String requesterName; // Nome de quem faz o pedido (se for terceiro)
    private String relationship; // Ex: filho, assistente social
    
    @NotBlank(message = "O nome de quem vai usar o equipamento é obrigatório.")
    private String beneficiaryName;

    @NotNull(message = "O tipo de documento é obrigatório.")
    private DocumentType documentType;

    @NotBlank(message = "O número do documento é obrigatório.")
    private String documentNumber;

    @NotBlank(message = "O WhatsApp para contato é obrigatório.")
    private String whatsapp;

    private String cep;
    private String street;
    private String number;
    private String neighborhood;
    private String city;
    private String state;
    private String complement;

    // Etapa 2: Escolha do Equipamento e Motivo
    @NotNull(message = "O tipo de equipamento é obrigatório.")
    private EquipmentType equipmentType;

    private Integer quantity; // Padrão: 1

    @NotBlank(message = "O motivo da necessidade do equipamento é obrigatório.")
    private String needReason; // Dado sensível de saúde protegido pela LGPD

    // Etapa 3: Consentimento (LGPD) e Modalidade
    @NotNull(message = "O consentimento LGPD é obrigatório.")
    private Boolean consentGranted;

    private boolean assistedRegistration; // RF02: Cadastrado pelo administrador/Márcio
}
