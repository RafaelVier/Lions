package org.lions.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.lions.backend.domain.enums.DocumentType;
import org.lions.backend.domain.enums.NotificationChannel;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tb_beneficiaries", indexes = {
        @Index(name = "idx_beneficiary_document", columnList = "document_number"),
        @Index(name = "idx_beneficiary_active", columnList = "active")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiaryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentType documentType;

    @Column(name = "document_number", nullable = false, length = 30)
    private String documentNumber;

    private LocalDate birthDate;

    @Column(length = 25)
    private String phone;

    @Column(length = 120)
    private String email;

    @Column(length = 10)
    private String cep;

    @Column(length = 150)
    private String street;

    @Column(name = "address_number", length = 20)
    private String number;

    @Column(length = 100)
    private String complement;

    @Column(length = 80)
    private String neighborhood;

    @Column(length = 80)
    private String city;

    @Column(length = 2)
    private String state;

    @Column(length = 150)
    private String alternativeContactName;

    @Column(length = 25)
    private String alternativeContactPhone;

    @Column(length = 60)
    private String alternativeContactRelationship;

    @Column(nullable = false)
    private boolean legallyIncapable;

    @Column(length = 150)
    private String legalGuardianName;

    @Column(length = 30)
    private String legalGuardianDocument;

    @Column(length = 25)
    private String legalGuardianPhone;

    @Column(length = 60)
    private String legalGuardianRelationship;

    @Column(nullable = false)
    private boolean consentGranted;

    private LocalDateTime consentTimestamp;

    @Column(length = 150)
    private String consentGivenBy;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_beneficiary_channels", joinColumns = @JoinColumn(name = "beneficiary_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    @Builder.Default
    private Set<NotificationChannel> notificationChannels = new HashSet<>();

    @Column(name = "active", nullable = false)
    private boolean active;

    private LocalDateTime inactivatedAt;

    @Column(length = 500)
    private String inactivationReason;

    @Column(nullable = false)
    private boolean anonymized;

    private LocalDateTime anonymizedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
