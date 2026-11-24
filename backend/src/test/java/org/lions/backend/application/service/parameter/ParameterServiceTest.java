package org.lions.backend.application.service.parameter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.lions.backend.application.dto.response.SystemParameterResponse;
import org.lions.backend.application.port.CurrentUserPort;
import org.lions.backend.application.service.audit.AuditService;
import org.lions.backend.domain.entity.EquipmentTypeConfig;
import org.lions.backend.domain.entity.SystemParameter;
import org.lions.backend.domain.enums.AuditAction;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.exception.BusinessException;
import org.lions.backend.domain.exception.ResourceNotFoundException;
import org.lions.backend.domain.repository.EquipmentTypeConfigRepositoryPort;
import org.lions.backend.domain.repository.SystemParameterRepositoryPort;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ParameterServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2025-03-10T12:00:00Z"), ZoneOffset.UTC);

    private final Map<String, SystemParameter> store = new HashMap<>();
    private final Map<EquipmentType, EquipmentTypeConfig> types = new HashMap<>();
    private AuditService auditService;
    private ParameterService service;

    @BeforeEach
    void setUp() {
        SystemParameterRepositoryPort repository = new SystemParameterRepositoryPort() {
            @Override
            public Optional<SystemParameter> findByKey(String key) {
                return Optional.ofNullable(store.get(key));
            }

            @Override
            public List<SystemParameter> findAll() {
                return new ArrayList<>(store.values());
            }

            @Override
            public SystemParameter save(SystemParameter parameter) {
                store.put(parameter.getKey(), parameter);
                return parameter;
            }
        };
        auditService = mock(AuditService.class);
        CurrentUserPort currentUser = mock(CurrentUserPort.class);
        when(currentUser.username()).thenReturn("marcio");
        EquipmentTypeConfigRepositoryPort typeRepository = new EquipmentTypeConfigRepositoryPort() {
            @Override
            public Optional<EquipmentTypeConfig> findByCode(EquipmentType code) {
                return Optional.ofNullable(types.get(code));
            }

            @Override
            public List<EquipmentTypeConfig> findAll() {
                return new ArrayList<>(types.values());
            }

            @Override
            public EquipmentTypeConfig save(EquipmentTypeConfig config) {
                types.put(config.getCode(), config);
                return config;
            }
        };
        service = new ParameterService(repository, typeRepository, auditService, currentUser, CLOCK);
    }

    private void stored(String key, String value) {
        store.put(key, SystemParameter.builder().key(key).value(value).build());
    }

    @Test
    void deveRetornarDefaultsQuandoNadaArmazenado() {
        assertThat(service.loanDefaultDays()).isEqualTo(30);
        assertThat(service.maxActiveLoansPerBeneficiary()).isEqualTo(2);
        assertThat(service.maxRenewals()).isEqualTo(2);
        assertThat(service.renewalWindowDays()).isEqualTo(7);
        assertThat(service.defaultEscalationDays()).isEqualTo(30);
        assertThat(service.reservationHours()).isEqualTo(48);
        assertThat(service.maxConsecutiveExpirations()).isEqualTo(2);
        assertThat(service.preventiveMonths()).isEqualTo(12);
        assertThat(service.preventiveLoanCycles()).isEqualTo(10);
        assertThat(service.notificationMaxAttempts()).isEqualTo(3);
        assertThat(service.overdueIntervalDays()).isEqualTo(3);
        assertThat(service.reportDefaultPeriodDays()).isEqualTo(30);
        assertThat(service.loanDays(EquipmentType.ANDADOR)).isEqualTo(30);
    }

    @Test
    void deveUsarValorArmazenadoQuandoExiste() {
        stored(ParameterService.LOAN_MAX_RENEWALS, "3");
        stored(ParameterService.LOAN_DEFAULT_DAYS, "60");
        types.put(EquipmentType.MULETAS, EquipmentTypeConfig.builder()
                .code(EquipmentType.MULETAS).description("Muletas").loanDays(15).build());
        types.put(EquipmentType.ANDADOR, EquipmentTypeConfig.defaultFor(EquipmentType.ANDADOR));

        assertThat(service.maxRenewals()).isEqualTo(3);
        assertThat(service.loanDays(EquipmentType.MULETAS)).isEqualTo(15);
        assertThat(service.loanDays(EquipmentType.ANDADOR)).isEqualTo(60);
    }

    @Test
    void deveUsarDefaultQuandoValorArmazenadoCorrompido() {
        stored(ParameterService.WAITLIST_RESERVATION_HOURS, "abc");
        assertThat(service.reservationHours()).isEqualTo(48);
    }

    @Test
    void deveListarTodosOsParametrosQuandoConsultado() {
        stored(ParameterService.LOAN_MAX_RENEWALS, "4");

        List<SystemParameterResponse> all = service.findAll();

        assertThat(all).hasSize(ParameterService.DEFINITIONS.size());
        SystemParameterResponse renewals = all.stream()
                .filter(p -> p.key().equals(ParameterService.LOAN_MAX_RENEWALS)).findFirst().orElseThrow();
        assertThat(renewals.value()).isEqualTo("4");
        assertThat(renewals.defaultValue()).isEqualTo("2");
        assertThat(renewals.customized()).isTrue();
        assertThat(all).noneMatch(p -> p.key().startsWith("loan.days."));
    }

    @Test
    void deveAtualizarEAuditarQuandoValorInteiroPositivo() {
        stored(ParameterService.LOAN_MAX_RENEWALS, "2");

        SystemParameterResponse response = service.update(ParameterService.LOAN_MAX_RENEWALS, " 5 ");

        assertThat(response.value()).isEqualTo("5");
        SystemParameter saved = store.get(ParameterService.LOAN_MAX_RENEWALS);
        assertThat(saved.getValue()).isEqualTo("5");
        assertThat(saved.getUpdatedBy()).isEqualTo("marcio");
        assertThat(saved.getUpdatedAt()).isEqualTo(LocalDateTime.of(2025, 3, 10, 12, 0));
        verify(auditService).record(eq(ParameterService.ENTITY_NAME), eq(ParameterService.LOAN_MAX_RENEWALS),
                eq(AuditAction.UPDATE), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", "1.5", "", "9999999999"})
    void deveRejeitarQuandoValorNaoInteiroPositivo(String value) {
        assertThatThrownBy(() -> service.update(ParameterService.LOAN_MAX_RENEWALS, value))
                .isInstanceOf(BusinessException.class);
        verify(auditService, never()).record(any(), any(), any(), any());
    }

    @Test
    void deveRetornarNaoEncontradoQuandoChaveDesconhecida() {
        assertThatThrownBy(() -> service.update("chave.inexistente", "1"))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.update("loan.days.ANDADOR", "1"))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(ParameterService.isKnownKey("loan.default-days")).isTrue();
        assertThat(ParameterService.isKnownKey(null)).isFalse();
    }
}
