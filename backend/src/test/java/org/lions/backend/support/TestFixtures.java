package org.lions.backend.support;

import org.lions.backend.application.dto.request.BeneficiaryRequest;
import org.lions.backend.domain.enums.NotificationChannel;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** Dados e autenticação para testes de integração dos serviços. */
public final class TestFixtures {

    private TestFixtures() {
    }

    /** Autentica o usuário com o perfil informado (ADMIN, OPERATOR, VIEWER) no contexto de segurança. */
    public static void authenticateAs(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                username, "n/a", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    /** CPF válido aleatório (dígitos verificadores corretos), para não colidir entre testes. */
    public static String randomCpf() {
        int[] d = new int[11];
        for (int i = 0; i < 9; i++) {
            d[i] = ThreadLocalRandom.current().nextInt(10);
        }
        for (int k = 9; k < 11; k++) {
            int sum = 0;
            for (int i = 0; i < k; i++) {
                sum += d[i] * (k + 1 - i);
            }
            int r = (sum * 10) % 11;
            d[k] = r == 10 ? 0 : r;
        }
        StringBuilder sb = new StringBuilder();
        for (int v : d) {
            sb.append(v);
        }
        return sb.toString();
    }

    /** Cadastro completo de beneficiário adulto, com consentimento e canal WhatsApp. */
    public static BeneficiaryRequest beneficiaryRequest(String name) {
        return new BeneficiaryRequest(name, randomCpf(), LocalDate.of(1950, 5, 20), "45999998888", null,
                "85884-000", "Rua Paraná", "100", null, "Centro", "Medianeira", "PR", "Contato " + name,
                "45988887777", "Filho", false, null, null, null, null, null, null, true, null,
                Set.of(NotificationChannel.WHATSAPP));
    }
}
