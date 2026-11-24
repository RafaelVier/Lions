package org.lions.backend.application.service.equipment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lions.backend.application.dto.request.EquipmentTypeUpdateRequest;
import org.lions.backend.application.dto.response.EquipmentTypeResponse;
import org.lions.backend.application.port.CurrentUserPort;
import org.lions.backend.application.service.audit.AuditService;
import org.lions.backend.application.service.parameter.ParameterService;
import org.lions.backend.domain.entity.EquipmentTypeConfig;
import org.lions.backend.domain.enums.AuditAction;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.exception.BusinessException;
import org.lions.backend.domain.exception.ResourceNotFoundException;
import org.lions.backend.domain.repository.EquipmentTypeConfigRepositoryPort;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EquipmentTypeServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2025-03-10T12:00:00Z"), ZoneOffset.UTC);

    private final Map<EquipmentType, EquipmentTypeConfig> store = new EnumMap<>(EquipmentType.class);
    private AuditService auditService;
    private EquipmentTypeService service;

    @BeforeEach
    void setUp() {
        EquipmentTypeConfigRepositoryPort repository = new EquipmentTypeConfigRepositoryPort() {
            @Override
            public Optional<EquipmentTypeConfig> findByCode(EquipmentType code) {
                return Optional.ofNullable(store.get(code));
            }

            @Override
            public List<EquipmentTypeConfig> findAll() {
                return new ArrayList<>(store.values());
            }

            @Override
            public EquipmentTypeConfig save(EquipmentTypeConfig config) {
                store.put(config.getCode(), config);
                return config;
            }
        };
        ParameterService parameterService = mock(ParameterService.class);
        when(parameterService.loanDefaultDays()).thenReturn(30);
        auditService = mock(AuditService.class);
        CurrentUserPort currentUser = mock(CurrentUserPort.class);
        when(currentUser.username()).thenReturn("marcio");
        service = new EquipmentTypeService(repository, parameterService, auditService, currentUser, CLOCK);
    }

    @Test
    void deveCriarUmRegistroPorTipoQuandoInicializaUmaVez() {
        assertThat(service.ensureDefaults()).isEqualTo(EquipmentType.values().length);
        assertThat(service.ensureDefaults()).isZero();
        assertThat(store.get(EquipmentType.MULETAS).getDescription()).isEqualTo("Muletas");
    }

    @Test
    void deveUsarPrazoPadraoQuandoTipoSemPrazo() {
        List<EquipmentTypeResponse> all = service.findAll();
        assertThat(all).hasSize(EquipmentType.values().length);
        assertThat(all).allMatch(t -> t.loanDays() == null && t.effectiveLoanDays() == 30);
    }

    @Test
    void deveAtualizarPrazoEAuditarQuandoAdminAltera() {
        EquipmentTypeResponse response = service.update("andador",
                new EquipmentTypeUpdateRequest("Andador articulado", 45));

        assertThat(response.code()).isEqualTo(EquipmentType.ANDADOR);
        assertThat(response.effectiveLoanDays()).isEqualTo(45);
        assertThat(store.get(EquipmentType.ANDADOR).getUpdatedBy()).isEqualTo("marcio");
        verify(auditService).record(eq(EquipmentTypeService.ENTITY_NAME), eq("ANDADOR"), eq(AuditAction.UPDATE),
                anyMap());

        EquipmentTypeResponse reset = service.update("ANDADOR", new EquipmentTypeUpdateRequest("Andador articulado", null));
        assertThat(reset.loanDays()).isNull();
        assertThat(reset.effectiveLoanDays()).isEqualTo(30);
    }

    @Test
    void deveRejeitarQuandoTipoInexistenteOuPrazoInvalido() {
        assertThatThrownBy(() -> service.update("FOGUETE", new EquipmentTypeUpdateRequest("x", 10)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.update("ANDADOR", new EquipmentTypeUpdateRequest("Andador", 0)))
                .isInstanceOf(BusinessException.class);
        verify(auditService, never()).record(any(), any(), any(), any());
    }
}
