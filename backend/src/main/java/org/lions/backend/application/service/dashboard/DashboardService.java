package org.lions.backend.application.service.dashboard;

import lombok.RequiredArgsConstructor;
import org.lions.backend.application.dto.response.DashboardResponse;
import org.lions.backend.application.dto.response.DashboardResponse.Occupancy;
import org.lions.backend.application.dto.response.DashboardResponse.OverdueItem;
import org.lions.backend.application.dto.response.DashboardResponse.TypeIndicator;
import org.lions.backend.application.port.CurrentUserPort;
import org.lions.backend.application.service.parameter.ParameterService;
import org.lions.backend.application.service.report.ReportPeriod;
import org.lions.backend.domain.entity.Equipment;
import org.lions.backend.domain.entity.Loan;
import org.lions.backend.domain.entity.Maintenance;
import org.lions.backend.domain.entity.MaintenanceCost;
import org.lions.backend.domain.entity.WaitlistEntry;
import org.lions.backend.domain.enums.EquipmentStatus;
import org.lions.backend.domain.enums.EquipmentType;
import org.lions.backend.domain.enums.LoanStatus;
import org.lions.backend.domain.enums.UserRole;
import org.lions.backend.domain.enums.WaitlistStatus;
import org.lions.backend.domain.repository.EquipmentRepositoryPort;
import org.lions.backend.domain.repository.LoanRepositoryPort;
import org.lions.backend.domain.repository.LoanReturnRepositoryPort;
import org.lions.backend.domain.repository.MaintenanceRepositoryPort;
import org.lions.backend.domain.repository.WaitlistEntryRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** Dashboard gerencial (RF15) com filtros de período e tipo (UC40) e visão por perfil (RN56). */
@Service
@RequiredArgsConstructor
public class DashboardService {

    static final int MAX_OVERDUE_ITEMS = 10;

    private final EquipmentRepositoryPort equipmentRepository;
    private final LoanRepositoryPort loanRepository;
    private final LoanReturnRepositoryPort returnRepository;
    private final MaintenanceRepositoryPort maintenanceRepository;
    private final WaitlistEntryRepositoryPort waitlistRepository;
    private final ParameterService parameterService;
    private final CurrentUserPort currentUser;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DashboardResponse build(LocalDate start, LocalDate end, EquipmentType type) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        ReportPeriod period = ReportPeriod.resolve(start, end, today, parameterService.reportDefaultPeriodDays());
        boolean personalDataHidden = !currentUser.hasRole(UserRole.ROLE_ADMIN)
                && !currentUser.hasRole(UserRole.ROLE_OPERATOR);

        List<Equipment> equipments = equipmentRepository.findAll();
        List<Loan> overdue = loanRepository.findOpenOverdue(today).stream()
                .filter(l -> l.getStatus() == LoanStatus.ATIVO || l.getStatus() == LoanStatus.ATRASADO)
                .toList();
        List<WaitlistEntry> waitlist = waitlistRepository.findAll().stream()
                .filter(w -> w.getStatus() == WaitlistStatus.AGUARDANDO || w.getStatus() == WaitlistStatus.RESERVADO)
                .toList();
        List<Maintenance> maintenances = maintenanceRepository.findAll();
        List<Loan> loansInPeriod = loanRepository.findByLoanDateBetween(period.start(), period.end());
        List<Loan> returnedLoans = returnRepository.findByReturnDateBetween(period.start(), period.end()).stream()
                .map(r -> loanRepository.findById(r.getLoanId()).orElse(null))
                .filter(Objects::nonNull)
                .toList();
        long openLoans = loanRepository.findByStatusIn(LoanStatus.openStatuses()).stream()
                .filter(l -> l.getStatus() != LoanStatus.PENDENTE_ASSINATURA)
                .filter(l -> matches(type, loanType(l)))
                .count();

        Predicate<EquipmentType> typeFilter = t -> matches(type, t);
        Occupancy occupancy = occupancy(equipments.stream().filter(e -> typeFilter.test(e.getType())).toList());
        long overdueCount = overdue.stream().filter(l -> typeFilter.test(loanType(l))).count();
        DashboardResponse.WaitlistSummary waitlistSummary = new DashboardResponse.WaitlistSummary(
                waitlist.stream().filter(w -> typeFilter.test(w.getEquipmentType()))
                        .filter(w -> w.getStatus() == WaitlistStatus.AGUARDANDO).count(),
                waitlist.stream().filter(w -> typeFilter.test(w.getEquipmentType()))
                        .filter(w -> w.getStatus() == WaitlistStatus.RESERVADO).count());
        BigDecimal cost = maintenanceCost(maintenances, period, typeFilter);
        DashboardResponse.LoanVolume volume = new DashboardResponse.LoanVolume(
                loansInPeriod.stream().filter(l -> typeFilter.test(loanType(l)))
                        .filter(l -> l.getStatus() != LoanStatus.CANCELADO).count(),
                loansInPeriod.stream().filter(l -> typeFilter.test(loanType(l)))
                        .filter(l -> l.getStatus() == LoanStatus.CANCELADO).count(),
                returnedLoans.stream().filter(l -> typeFilter.test(loanType(l))).count(),
                openLoans);

        List<TypeIndicator> byType = Arrays.stream(EquipmentType.values())
                .filter(typeFilter)
                .map(t -> {
                    Predicate<EquipmentType> only = t::equals;
                    Occupancy o = occupancy(equipments.stream().filter(e -> e.getType() == t).toList());
                    return new TypeIndicator(t, t.getDescription(), o.activeEquipments(), o.loaned(), o.occupancyRate(),
                            overdue.stream().filter(l -> loanType(l) == t).count(),
                            waitlist.stream().filter(w -> w.getEquipmentType() == t
                                    && w.getStatus() == WaitlistStatus.AGUARDANDO).count(),
                            maintenanceCost(maintenances, period, only),
                            loansInPeriod.stream().filter(l -> loanType(l) == t)
                                    .filter(l -> l.getStatus() != LoanStatus.CANCELADO).count());
                })
                .toList();

        List<OverdueItem> overdueItems = personalDataHidden ? null : overdue.stream()
                .filter(l -> typeFilter.test(loanType(l)))
                .map(l -> new OverdueItem(l.getId(), l.getBeneficiary().getFullName(),
                        l.getEquipment() != null ? l.getEquipment().getPatrimonyNumber() : null, loanType(l),
                        l.getExpectedReturnDate(), l.daysOverdue(today)))
                .sorted(Comparator.comparingLong(OverdueItem::daysOverdue).reversed())
                .limit(MAX_OVERDUE_ITEMS)
                .toList();

        return new DashboardResponse(period.start(), period.end(), type, now, personalDataHidden, occupancy,
                overdueCount, waitlistSummary, cost, volume, byType, overdueItems);
    }

    /** RN54/RN55 — ocupação sobre os equipamentos ativos (não baixados). */
    static Occupancy occupancy(List<Equipment> equipments) {
        long writtenOff = count(equipments, EquipmentStatus.BAIXADO);
        long active = equipments.size() - writtenOff;
        long loaned = count(equipments, EquipmentStatus.EMPRESTADO);
        return new Occupancy(active, count(equipments, EquipmentStatus.DISPONIVEL),
                count(equipments, EquipmentStatus.RESERVADO), loaned, count(equipments, EquipmentStatus.EM_MANUTENCAO),
                writtenOff, rate(loaned, active));
    }

    static BigDecimal rate(long part, long total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(part * 100.0 / total).setScale(1, RoundingMode.HALF_UP);
    }

    /** RN38/RN55 — custos lançados no período (inclui ajustes e itens já baixados). */
    private static BigDecimal maintenanceCost(List<Maintenance> maintenances, ReportPeriod period,
                                              Predicate<EquipmentType> typeFilter) {
        return maintenances.stream()
                .filter(m -> m.getEquipment() != null && typeFilter.test(m.getEquipment().getType()))
                .flatMap(m -> m.getCosts().stream())
                .filter(c -> c.getRegisteredAt() != null && period.contains(c.getRegisteredAt().toLocalDate()))
                .map(MaintenanceCost::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static long count(List<Equipment> equipments, EquipmentStatus status) {
        return equipments.stream().filter(e -> e.getStatus() == status).count();
    }

    private static EquipmentType loanType(Loan loan) {
        return loan.getEquipment() != null ? loan.getEquipment().getType() : null;
    }

    private static boolean matches(EquipmentType filter, EquipmentType value) {
        return filter == null || filter == value;
    }
}
