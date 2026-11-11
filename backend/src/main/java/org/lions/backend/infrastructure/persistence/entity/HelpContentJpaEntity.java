package org.lions.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.lions.backend.domain.enums.HelpAudience;
import org.lions.backend.domain.enums.HelpContentType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tb_help_contents", indexes = {
        @Index(name = "idx_help_content_active", columnList = "active")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HelpContentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HelpContentType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 500)
    private String keywords;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_help_content_audiences", joinColumns = @JoinColumn(name = "help_content_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "audience", nullable = false, length = 20)
    @Builder.Default
    private Set<HelpAudience> audiences = new HashSet<>();

    @Column(nullable = false)
    private int displayOrder;

    @Column(name = "active", nullable = false)
    private boolean active;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
