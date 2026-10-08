package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TermAcceptanceRequest {

    @NotBlank(message = "O nome de quem aceita o termo é obrigatório.")
    private String acceptedByName;
}
