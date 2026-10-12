package org.lions.backend.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lions.backend.domain.enums.UserRole;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "O nome de usuário é obrigatório.")
    private String username;

    @NotBlank(message = "O email é obrigatório.")
    @Email(message = "Email inválido.")
    private String email;

    @NotBlank(message = "A senha é obrigatória.")
    private String password;

    private UserRole role; // Default: ROLE_VOLUNTEER
}
