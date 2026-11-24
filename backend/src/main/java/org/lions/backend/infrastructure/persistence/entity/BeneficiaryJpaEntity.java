package org.lions.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
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
        @Index(name = "idx_beneficiary_cpf", columnList = "cpf"),
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

    /** RN04 — somente dígitos; nulo após anonimização (RN59). Único entre ativos (índice parcial no PostgreSQL). */
    @Column(length = 11)
    private String cpf;

    private LocalDate birthDate;

    @Column(length = 25)
    private String phone;

    @Column(length = 120)
    private String email;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", unique = true)
    private AddressJpaEntity address;

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

    @Column(length = 11)
    private String legalGuardianCpf;

    @Column(length = 25)
    private String legalGuardianPhone;

    @Column(length = 120)
    private String legalGuardianEmail;

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
