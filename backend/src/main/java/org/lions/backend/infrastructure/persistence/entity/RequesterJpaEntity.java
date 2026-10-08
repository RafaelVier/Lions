package org.lions.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.lions.backend.domain.enums.DocumentType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_requesters")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequesterJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean isForSelf;

    @Column(length = 120)
    private String requesterName;

    @Column(length = 80)
    private String relationship;

    @Column(nullable = false, length = 120)
    private String beneficiaryName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentType documentType;

    @Column(nullable = false, length = 25)
    private String documentNumber;

    @Column(nullable = false, length = 25)
    private String whatsapp;

    @Column(length = 10)
    private String cep;

    @Column(length = 150)
    private String street;

    @Column(length = 20)
    private String number;

    @Column(length = 80)
    private String neighborhood;

    @Column(length = 80)
    private String city;

    @Column(length = 2)
    private String state;

    @Column(length = 100)
    private String complement;

    @Column(columnDefinition = "TEXT")
    private String needReason; // LGPD Dado sensível

    @Column(nullable = false)
    private boolean consentGranted;

    private LocalDateTime consentTimestamp;

    @Column(nullable = false)
    private boolean assistedRegistration;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
