package org.lions.backend.application.service.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lions.backend.application.port.CurrentUserPort;
import org.lions.backend.domain.entity.AuditLog;
import org.lions.backend.domain.entity.Equipment;
import org.lions.backend.domain.enums.AuditAction;
import org.lions.backend.domain.enums.EquipmentCondition;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.repository.AuditLogRepositoryPort;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2025-03-10T13:00:00Z"), ZONE);

    private AuditLogRepositoryPort repository;
    private CurrentUserPort currentUser;
    private AuditService service;

    @BeforeEach
    void setUp() {
        repository = mock(AuditLogRepositoryPort.class);
        currentUser = mock(CurrentUserPort.class);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service = new AuditService(repository, currentUser, CLOCK, new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void deveRegistrarComUsuarioCorrenteQuandoAcaoManual() {
        when(currentUser.username()).thenReturn("marcio");

        service.record("Equipment", 7L, AuditAction.UPDATE, Map.of("status", "BAIXADO"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertThat(log.getEntityName()).isEqualTo("Equipment");
        assertThat(log.getEntityId()).isEqualTo("7");
        assertThat(log.getAction()).isEqualTo(AuditAction.UPDATE);
        assertThat(log.getUsername()).isEqualTo("marcio");
        assertThat(log.isAutomatic()).isFalse();
        assertThat(log.getOriginRule()).isNull();
        assertThat(log.getDetails()).isEqualTo("{\"status\":\"BAIXADO\"}");
        assertThat(log.getOccurredAt()).isEqualTo(LocalDateTime.of(2025, 3, 10, 10, 0));
    }

    @Test
    void deveRegistrarComoSistemaQuandoAcaoAutomatica() {
        AuditLog log = service.recordAutomatic("Loan", 3L, AuditAction.STATUS_CHANGE, "RN27", "ATIVO -> ATRASADO");

        assertThat(log.getUsername()).isEqualTo(CurrentUserPort.SYSTEM_USERNAME);
        assertThat(log.isAutomatic()).isTrue();
        assertThat(log.getOriginRule()).isEqualTo("RN27");
        assertThat(log.getDetails()).isEqualTo("ATIVO -> ATRASADO");
    }

    @Test
    void deveRetornarSomenteCamposAlteradosQuandoDiffComCamposInformados() {
        Equipment before = Equipment.builder().id(1L).status(EquipmentStatus.DISPONIVEL)
                .condition(EquipmentCondition.BOM).notes("a").build();
        Equipment after = Equipment.builder().id(1L).status(EquipmentStatus.EM_MANUTENCAO)
                .condition(EquipmentCondition.BOM).notes("b").build();

        Map<String, Map<String, Object>> diff = service.diff(before, after, "status", "condition");

        assertThat(diff).containsOnlyKeys("status");
        assertThat(diff.get("status")).containsEntry("old", EquipmentStatus.DISPONIVEL)
                .containsEntry("new", EquipmentStatus.EM_MANUTENCAO);
    }

    @Test
    void deveCompararPropriedadesSimplesQuandoDiffSemCampos() {
        Equipment before = Equipment.builder().id(1L).notes("a").version(1L)
                .updatedAt(LocalDateTime.of(2025, 1, 1, 0, 0)).build();
        Equipment after = Equipment.builder().id(1L).notes("b").version(2L)
                .updatedAt(LocalDateTime.of(2025, 2, 1, 0, 0)).build();

        Map<String, Map<String, Object>> diff = service.diff(before, after);

        assertThat(diff).containsOnlyKeys("notes");
    }

    @Test
    void deveIgnorarEscalaQuandoDiffDeBigDecimal() {
        assertThat(service.diff(new Cost(new BigDecimal("10.0")), new Cost(new BigDecimal("10.00")), "amount"))
                .isEmpty();
        assertThat(service.diff(null, new Cost(BigDecimal.ONE), "amount")).containsKey("amount");
    }

    public static class Cost {
        private final BigDecimal amount;

        public Cost(BigDecimal amount) {
            this.amount = amount;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }
}
