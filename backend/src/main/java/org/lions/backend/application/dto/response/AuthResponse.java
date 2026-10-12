package org.lions.backend.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lions.backend.domain.enums.UserRole;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String type = "Bearer";
    private String username;
    private String email;
    private UserRole role;

    public AuthResponse(String token, String username, String email, UserRole role) {
        this.token = token;
        this.type = "Bearer";
        this.username = username;
        this.email = email;
        this.role = role;
    }
}
