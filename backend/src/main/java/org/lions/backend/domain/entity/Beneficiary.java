package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lions.backend.application.support.CpfValidator;
import org.lions.backend.application.support.LgpdMasker;
import org.lions.backend.domain.enums.NotificationChannel;
import org.lions.backend.domain.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Assistido/beneficiário do banco ortopédico (substitui o antigo {@code Requester}).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Beneficiary {

    public static final int ADULT_AGE = 18;

    private Long id;
    private String fullName;
    /** RN04 — CPF (somente dígitos) é a identificação única do beneficiário ativo; nulo após anonimização. */
    private String cpf;
    private LocalDate birthDate;
    private String phone;
    private String email;

    private String cep;
    private String street;
    private String number;
    private String complement;
    private String neighborhood;
    private String city;
    private String state;

    private String alternativeContactName;
    private String alternativeContactPhone;
    private String alternativeContactRelationship;

    private boolean legallyIncapable;
    private String legalGuardianName;
    private String legalGuardianCpf;
    private String legalGuardianPhone;
    private String legalGuardianEmail;
    private String legalGuardianRelationship;

    private boolean consentGranted;
    private LocalDateTime consentTimestamp;
    private String consentGivenBy;
    @Builder.Default
    private Set<NotificationChannel> notificationChannels = EnumSet.noneOf(NotificationChannel.class);

    @Builder.Default
    private boolean active = true;
    private LocalDateTime inactivatedAt;
    private String inactivationReason;
    private boolean anonymized;
    private LocalDateTime anonymizedAt;
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Valida o cadastro: RN05 (obrigatórios), RN04 (CPF válido), RN06 (responsável legal) e RN07 (consentimento).
     * Normaliza os CPFs (apenas dígitos).
     */
    public void validate(LocalDate today) {
        if (notificationChannels == null) {
            notificationChannels = EnumSet.noneOf(NotificationChannel.class);
        }
        if (cpf != null) {
            cpf = CpfValidator.normalize(cpf);
        }
        if (legalGuardianCpf != null) {
            legalGuardianCpf = CpfValidator.normalize(legalGuardianCpf);
        }

        List<String> missing = new ArrayList<>();
        requireText(fullName, "nome completo", missing);
        requireText(cpf, "CPF", missing);
        if (birthDate == null) {
            missing.add("data de nascimento");
        }
        requireText(phone, "telefone", missing);
        requireText(cep, "CEP", missing);
        requireText(street, "logradouro", missing);
        requireText(number, "número", missing);
        requireText(neighborhood, "bairro", missing);
        requireText(city, "cidade", missing);
        requireText(state, "UF", missing);
        requireText(alternativeContactName, "nome do contato alternativo", missing);
        requireText(alternativeContactPhone, "telefone do contato alternativo", missing);
        if (!missing.isEmpty()) {
            throw new BusinessException("Campos obrigatórios não informados: " + String.join(", ", missing) + ".");
        }

        if (birthDate.isAfter(today)) {
            throw new BusinessException("Data de nascimento não pode ser futura.");
        }
        if (!CpfValidator.isValid(cpf)) {
            throw new BusinessException("CPF inválido.");
        }

        if (requiresLegalGuardian(today)) {
            List<String> guardianMissing = new ArrayList<>();
            requireText(legalGuardianName, "nome do responsável legal", guardianMissing);
            requireText(legalGuardianCpf, "CPF do responsável legal", guardianMissing);
            if (!guardianMissing.isEmpty()) {
                throw new BusinessException("Beneficiário menor de idade ou legalmente incapaz exige responsável legal: "
                        + String.join(", ", guardianMissing) + ".");
            }
        }
        if (legalGuardianCpf != null && !CpfValidator.isValid(legalGuardianCpf)) {
            throw new BusinessException("CPF do responsável legal inválido.");
        }

        if (!consentGranted || consentTimestamp == null) {
            throw new BusinessException("O cadastro só pode ser efetivado com o consentimento (LGPD) registrado.");
        }
    }

    /** RN07 — registra o consentimento com a data/hora do sistema e os canais autorizados (pode ser vazio). */
    public void grantConsent(Set<NotificationChannel> channels, String givenBy, LocalDateTime now) {
        this.consentGranted = true;
        this.consentTimestamp = now;
        this.consentGivenBy = givenBy;
        this.notificationChannels = channels == null || channels.isEmpty()
                ? EnumSet.noneOf(NotificationChannel.class)
                : EnumSet.copyOf(channels);
    }

    public boolean isMinor(LocalDate today) {
        return birthDate != null && Period.between(birthDate, today).getYears() < ADULT_AGE;
    }

    /** RN06 — menor de 18 anos ou legalmente incapaz. */
    public boolean requiresLegalGuardian(LocalDate today) {
        return legallyIncapable || isMinor(today);
    }

    /** Quem assina o termo (RN06): o responsável legal, quando exigido; senão o próprio beneficiário. */
    public String signatoryName(LocalDate today) {
        return requiresLegalGuardian(today) ? legalGuardianName : fullName;
    }

    public String signatoryCpf(LocalDate today) {
        return requiresLegalGuardian(today) ? legalGuardianCpf : cpf;
    }

    public void inactivate(String reason, LocalDateTime now) {
        if (!active) {
            throw new BusinessException("Beneficiário já está inativo.");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Motivo da inativação é obrigatório.");
        }
        this.active = false;
        this.inactivatedAt = now;
        this.inactivationReason = reason;
    }

    public void reactivate(LocalDateTime now) {
        if (anonymized) {
            throw new BusinessException("Beneficiário anonimizado não pode ser reativado.");
        }
        if (active) {
            throw new BusinessException("Beneficiário já está ativo.");
        }
        this.active = true;
        this.inactivatedAt = null;
        this.inactivationReason = null;
    }

    /** RN59 — remove/substitui dados pessoais preservando o histórico (empréstimos, ocorrências etc.). */
    public void anonymize(LocalDateTime now) {
        if (anonymized) {
            throw new BusinessException("Beneficiário já foi anonimizado.");
        }
        String suffix = id == null ? "" : String.valueOf(id);
        this.fullName = "Beneficiário anonimizado " + suffix;
        this.cpf = null;
        this.birthDate = null;
        this.phone = null;
        this.email = null;
        this.cep = null;
        this.street = null;
        this.number = null;
        this.complement = null;
        this.neighborhood = null;
        this.city = null;
        this.state = null;
        this.alternativeContactName = null;
        this.alternativeContactPhone = null;
        this.alternativeContactRelationship = null;
        this.legalGuardianName = null;
        this.legalGuardianCpf = null;
        this.legalGuardianPhone = null;
        this.legalGuardianEmail = null;
        this.legalGuardianRelationship = null;
        this.notes = null;
        this.notificationChannels = EnumSet.noneOf(NotificationChannel.class);
        if (active) {
            this.active = false;
            this.inactivatedAt = now;
            this.inactivationReason = "Anonimização (LGPD)";
        }
        this.anonymized = true;
        this.anonymizedAt = now;
    }

    public String getMaskedCpf() {
        return LgpdMasker.maskDocument(cpf);
    }

    private static void requireText(String value, String label, List<String> missing) {
        if (value == null || value.isBlank()) {
            missing.add(label);
        }
    }
}
