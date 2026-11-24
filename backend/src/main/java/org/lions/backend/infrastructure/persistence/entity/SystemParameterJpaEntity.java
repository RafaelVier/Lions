package org.lions.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_system_parameters")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemParameterJpaEntity {

    @Id
    @Column(name = "param_key", length = 100)
    private String key;

    @Column(name = "param_value", nullable = false, length = 500)
    private String value;

    @Column(length = 500)
    private String description;

    @Column(length = 50)
    private String updatedBy;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
