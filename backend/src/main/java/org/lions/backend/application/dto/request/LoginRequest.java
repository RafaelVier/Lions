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
public class LoginRequest {

    @NotBlank(message = "O usuário ou email é obrigatório.")
    private String username;

    @NotBlank(message = "A senha é obrigatória.")
    private String password;
}
