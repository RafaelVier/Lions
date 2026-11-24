package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Parâmetro de sistema chave/valor (prazos, limites) editável pelo Admin. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemParameter {

    private String key;
    private String value;
    private String description;
    private String updatedBy;
    private LocalDateTime updatedAt;

    public void update(String newValue, String by, LocalDateTime now) {
        this.value = newValue;
        this.updatedBy = by;
        this.updatedAt = now;
    }
}
