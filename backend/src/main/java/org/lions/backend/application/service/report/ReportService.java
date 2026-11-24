package org.lions.backend.application.service.report;

import lombok.RequiredArgsConstructor;
import org.lions.backend.application.port.CurrentUserPort;
import org.lions.backend.application.service.audit.AuditService;
import org.lions.backend.application.service.parameter.ParameterService;
import org.lions.backend.application.support.LgpdMasker;
import org.lions.backend.domain.entity.Beneficiary;
import org.lions.backend.domain.entity.Equipment;
import org.lions.backend.domain.entity.Loan;
import org.lions.backend.domain.entity.Maintenance;
import org.lions.backend.domain.enums.AuditAction;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.LoanStatus;
import org.lions.backend.domain.enums.ReportFormat;
import org.lions.backend.domain.enums.ReportType;
import org.lions.backend.domain.enums.UserRole;
import org.lions.backend.domain.repository.EquipmentRepositoryPort;
import org.lions.backend.domain.repository.LoanRepositoryPort;
import org.lions.backend.domain.repository.MaintenanceRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Relatórios (RF11, UC28/UC29) em CSV ou PDF:
 * <ul>
 *   <li>{@code INVENTARIO}: acervo atual, inclusive baixados, com custo acumulado de manutenção (RN38, RN55) — retrato
 *   da base, sem período;</li>
 *   <li>{@code EMPRESTIMOS}, {@code MANUTENCOES} e {@code ASSISTIDOS}: transacionais, exigem período (padrão: últimos
 *   30 dias — RN45).</li>
 * </ul>
 * Perfil Consulta recebe nome, CPF, telefone e endereço mascarados (RN43). Toda exportação é auditada com usuário,
 * data/hora e filtros (RN44).
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    public static final String ENTITY_NAME = "Report";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final EquipmentRepositoryPort equipmentRepository;
    private final LoanRepositoryPort loanRepository;
    private final MaintenanceRepositoryPort maintenanceRepository;
    private final ParameterService parameterService;
    private final AuditService auditService;
    private final CurrentUserPort currentUser;
    private final Clock clock;

    /** Arquivo gerado. */
    public record ReportFile(String fileName, String contentType, byte[] content, int rows) {
    }

    @Transactional
    public ReportFile generate(ReportType type, ReportFormat format, LocalDate start, LocalDate end,
                               EquipmentType equipmentType) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        boolean masked = !currentUser.hasRole(UserRole.ROLE_ADMIN) && !currentUser.hasRole(UserRole.ROLE_OPERATOR);
        ReportPeriod period = type == ReportType.INVENTARIO ? null
                : ReportPeriod.resolve(start, end, today, parameterService.reportDefaultPeriodDays());

        ReportTable table = switch (type) {
            case INVENTARIO -> inventory(equipmentType, masked);
            case EMPRESTIMOS -> loans(period, equipmentType, masked, today);
            case MANUTENCOES -> maintenances(period, equipmentType, masked);
            case ASSISTIDOS -> beneficiaries(period, equipmentType, masked, today);
        };
        ReportFormat effectiveFormat = format != null ? format : ReportFormat.CSV;
        byte[] content = effectiveFormat == ReportFormat.PDF
                ? PdfReportWriter.write(table, "Gerado em " + DATE_TIME.format(now) + " por " + currentUser.username())
                : CsvReportWriter.write(table);

        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("type", type);
        filters.put("format", effectiveFormat);
        filters.put("start", period != null ? period.start() : null);
        filters.put("end", period != null ? period.end() : null);
        filters.put("equipmentType", equipmentType);
        filters.put("rows", table.rows().size());
        filters.put("personalDataMasked", masked);
        auditService.record(ENTITY_NAME, type.name(), AuditAction.EXPORT, filters);

        String fileName = "relatorio-" + type.name().toLowerCase(Locale.ROOT) + "-" + FILE_DATE.format(today)
                + (effectiveFormat == ReportFormat.PDF ? ".pdf" : ".csv");
        return new ReportFile(fileName,
                effectiveFormat == ReportFormat.PDF ? PdfReportWriter.CONTENT_TYPE : CsvReportWriter.CONTENT_TYPE,
                content, table.rows().size());
    }

    // ---------- relatórios ----------

    private ReportTable inventory(EquipmentType equipmentType, boolean masked) {
        Map<Long, List<Maintenance>> maintenancesByEquipment = maintenanceRepository.findAll().stream()
                .filter(m -> m.getEquipment() != null)
                .collect(Collectors.groupingBy(m -> m.getEquipment().getId()));
        Map<Long, Long> loansByEquipment = loanRepository.findAll().stream()
                .filter(l -> l.getEquipment() != null && l.getStatus() != LoanStatus.CANCELADO)
                .collect(Collectors.groupingBy(l -> l.getEquipment().getId(), Collectors.counting()));
        List<List<String>> rows = equipmentRepository.findAll().stream()
                .filter(e -> equipmentType == null || e.getType() == equipmentType)
                .sorted(Comparator.comparing(Equipment::getPatrimonyNumber, Comparator.nullsLast(String::compareTo)))
                .map(e -> {
                    List<Maintenance> ms = maintenancesByEquipment.getOrDefault(e.getId(), List.of());
                    BigDecimal cost = ms.stream().map(Maintenance::totalCost).filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return row(e.getPatrimonyNumber(), e.getType().getDescription(), e.getSize(),
                            e.getManufactureYear() != null ? String.valueOf(e.getManufactureYear()) : null,
                            e.getCondition().getDescription(), e.getStatus().getDescription(),
                            date(e.getCreatedAt() != null ? e.getCreatedAt().toLocalDate() : null),
                            date(e.getWriteOffDate()),
                            e.getWriteOffReason() != null ? e.getWriteOffReason().getDescription() : null,
                            String.valueOf(loansByEquipment.getOrDefault(e.getId(), 0L)),
                            String.valueOf(ms.size()), money(cost));
                })
                .toList();
        long writtenOff = equipmentRepository.findAll().stream()
                .filter(e -> equipmentType == null || e.getType() == equipmentType)
                .filter(e -> e.getStatus() == EquipmentStatus.BAIXADO).count();
        return new ReportTable("Relatório de inventário", filters(null, equipmentType, masked),
                List.of("Patrimônio", "Tipo", "Porte", "Ano", "Conservação", "Status", "Cadastro", "Baixa",
                        "Motivo da baixa", "Empréstimos", "Manutenções", "Custo de manutenção (R$)"),
                rows, "Inclui " + writtenOff + " equipamento(s) baixado(s), mantidos no histórico (RN55).");
    }

    private ReportTable loans(ReportPeriod period, EquipmentType equipmentType, boolean masked, LocalDate today) {
        List<List<String>> rows = loanRepository.findByLoanDateBetween(period.start(), period.end()).stream()
                .filter(l -> equipmentType == null || type(l) == equipmentType)
                .sorted(Comparator.comparing(Loan::getLoanDate).thenComparing(Loan::getId))
                .map(l -> {
                    Beneficiary b = l.getBeneficiary();
                    long daysOverdue = l.isOpen() && l.isOverdue(today) ? l.daysOverdue(today) : 0;
                    return row(String.valueOf(l.getId()), date(l.getLoanDate()), date(l.getExpectedReturnDate()),
                            date(l.getReturnedAt() != null ? l.getReturnedAt().toLocalDate() : null),
                            l.getStatus().getDescription(), patrimony(l), type(l) != null ? type(l).getDescription() : null,
                            name(b, masked), cpf(b, masked), String.valueOf(l.getRenewalCount()),
                            l.isRetroactive() ? "Sim" : "Não", daysOverdue > 0 ? String.valueOf(daysOverdue) : "");
                })
                .toList();
        return new ReportTable("Relatório de empréstimos", filters(period, equipmentType, masked),
                List.of("Nº", "Retirada", "Devolução prevista", "Devolvido em", "Status", "Patrimônio", "Tipo",
                        "Beneficiário", "CPF", "Renovações", "Retroativo", "Dias de atraso"),
                rows, maskNote(masked));
    }

    private ReportTable maintenances(ReportPeriod period, EquipmentType equipmentType, boolean masked) {
        List<List<String>> rows = maintenanceRepository.findByEntryDateBetween(period.start(), period.end()).stream()
                .filter(m -> m.getEquipment() != null)
                .filter(m -> equipmentType == null || m.getEquipment().getType() == equipmentType)
                .sorted(Comparator.comparing(Maintenance::getEntryDate).thenComparing(Maintenance::getId))
                .map(m -> row(String.valueOf(m.getId()), m.getEquipment().getPatrimonyNumber(),
                        m.getEquipment().getType().getDescription(), m.getType().getDescription(),
                        m.getStatus().getDescription(), date(m.getEntryDate()), date(m.getExitDate()),
                        m.getTechnicalResponsible(), m.getDefectDescription(),
                        m.getCosts().stream().map(c -> c.getPartReplaced()).filter(Objects::nonNull)
                                .collect(Collectors.joining(", ")),
                        money(m.totalCost())))
                .toList();
        BigDecimal total = maintenanceRepository.findByEntryDateBetween(period.start(), period.end()).stream()
                .filter(m -> m.getEquipment() != null)
                .filter(m -> equipmentType == null || m.getEquipment().getType() == equipmentType)
                .map(Maintenance::totalCost).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ReportTable("Relatório de manutenções", filters(period, equipmentType, masked),
                List.of("Nº", "Patrimônio", "Tipo de equipamento", "Tipo", "Status", "Entrada", "Saída",
                        "Responsável técnico", "Defeito", "Peças trocadas", "Custo (R$)"),
                rows, "Custo total no período: R$ " + money(total) + ".");
    }

    /** Perfil dos assistidos atendidos no período (com empréstimo registrado no período). */
    private ReportTable beneficiaries(ReportPeriod period, EquipmentType equipmentType, boolean masked,
                                     LocalDate today) {
        Map<Long, List<Loan>> loansByBeneficiary = loanRepository.findByLoanDateBetween(period.start(), period.end())
                .stream()
                .filter(l -> l.getStatus() != LoanStatus.CANCELADO)
                .filter(l -> equipmentType == null || type(l) == equipmentType)
                .collect(Collectors.groupingBy(l -> l.getBeneficiary().getId(), LinkedHashMap::new, Collectors.toList()));
        List<List<String>> rows = new ArrayList<>();
        loansByBeneficiary.values().stream()
                .sorted(Comparator.comparing(ls -> ls.get(0).getBeneficiary().getFullName(),
                        Comparator.nullsLast(String::compareToIgnoreCase)))
                .forEach(ls -> {
                    Beneficiary b = ls.get(0).getBeneficiary();
                    Integer age = b.getBirthDate() != null ? Period.between(b.getBirthDate(), today).getYears() : null;
                    rows.add(row(String.valueOf(b.getId()), name(b, masked), cpf(b, masked),
                            age != null ? String.valueOf(age) : null, ageRange(age),
                            masked ? LgpdMasker.maskPhone(b.getPhone()) : b.getPhone(), address(b, masked),
                            b.isActive() ? "Ativo" : "Inativo", String.valueOf(ls.size()),
                            String.valueOf(ls.stream().filter(Loan::isOpen).count()),
                            ls.stream().map(ReportService::type).filter(Objects::nonNull).distinct()
                                    .map(EquipmentType::getDescription).collect(Collectors.joining(", "))));
                });
        return new ReportTable("Relatório de assistidos atendidos", filters(period, equipmentType, masked),
                List.of("Nº", "Nome", "CPF", "Idade", "Faixa etária", "Telefone", "Endereço", "Cadastro",
                        "Empréstimos no período", "Em aberto", "Tipos de equipamento"),
                rows, maskNote(masked));
    }

    // ---------- formatação ----------

    private static List<String> row(String... values) {
        List<String> row = new ArrayList<>(values.length);
        for (String value : values) {
            row.add(value == null ? "" : value);
        }
        return row;
    }

    private static List<String> filters(ReportPeriod period, EquipmentType equipmentType, boolean masked) {
        List<String> filters = new ArrayList<>();
        filters.add(period != null
                ? "Período: " + DATE.format(period.start()) + " a " + DATE.format(period.end())
                : "Situação atual do acervo");
        filters.add("Tipo de equipamento: " + (equipmentType != null ? equipmentType.getDescription() : "todos"));
        if (masked) {
            filters.add("Dados pessoais mascarados (perfil Consulta — RN43)");
        }
        return filters;
    }

    private static String maskNote(boolean masked) {
        return masked ? "Nome, CPF, telefone e endereço mascarados para o perfil Consulta (RN43)." : null;
    }

    private static String name(Beneficiary b, boolean masked) {
        return b == null ? null : masked ? LgpdMasker.maskName(b.getFullName()) : b.getFullName();
    }

    private static String cpf(Beneficiary b, boolean masked) {
        if (b == null || b.getCpf() == null) {
            return null;
        }
        if (masked) {
            return b.getMaskedCpf();
        }
        String c = b.getCpf();
        return c.length() == 11 ? c.substring(0, 3) + "." + c.substring(3, 6) + "." + c.substring(6, 9) + "-"
                + c.substring(9) : c;
    }

    private static String address(Beneficiary b, boolean masked) {
        if (masked) {
            return LgpdMasker.maskAddress(b.getCity(), b.getState());
        }
        StringBuilder sb = new StringBuilder();
        if (b.getStreet() != null) {
            sb.append(b.getStreet());
            if (b.getNumber() != null) {
                sb.append(", ").append(b.getNumber());
            }
            sb.append(" - ");
        }
        if (b.getNeighborhood() != null) {
            sb.append(b.getNeighborhood()).append(" - ");
        }
        if (b.getCity() != null) {
            sb.append(b.getCity());
        }
        if (b.getState() != null) {
            sb.append('/').append(b.getState());
        }
        return sb.toString();
    }

    static String ageRange(Integer age) {
        if (age == null) {
            return null;
        }
        if (age < 18) {
            return "0–17";
        }
        if (age < 60) {
            return "18–59";
        }
        if (age < 80) {
            return "60–79";
        }
        return "80+";
    }

    private static String date(LocalDate date) {
        return date == null ? null : DATE.format(date);
    }

    private static String money(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(PT_BR);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format((value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP));
    }

    private static String patrimony(Loan loan) {
        return loan.getEquipment() != null ? loan.getEquipment().getPatrimonyNumber() : null;
    }

    private static EquipmentType type(Loan loan) {
        return loan.getEquipment() != null ? loan.getEquipment().getType() : null;
    }
}
