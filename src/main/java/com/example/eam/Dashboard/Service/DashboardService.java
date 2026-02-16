package com.example.eam.Dashboard.Service;

import com.example.eam.Dashboard.Dto.*;
import com.example.eam.Enum.AssetCriticality;
import com.example.eam.Enum.ServiceRequestStatus;
import com.example.eam.Enum.WorkOrderStatus;
import com.example.eam.Enum.WorkType;
import com.example.eam.Procurement.Enum.MaterialRequisitionStatus;
import com.example.eam.Procurement.Repository.MaterialRequisitionRepository;
import com.example.eam.Technician.Repository.TechnicianLeaveRepository;
import com.example.eam.Technician.Repository.TechnicianHolidayRepository;
import com.example.eam.Technician.Repository.TechnicianRepository;
import com.example.eam.ServiceMaintenance.Repository.ServiceMaintenanceRepository;
import com.example.eam.WorkOrder.Entity.WorkOrder;
import com.example.eam.WorkOrder.Repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final Set<WorkOrderStatus> ACTIVE_WORK_ORDER_STATUSES =
            EnumSet.of(WorkOrderStatus.NEW, WorkOrderStatus.APPROVED, WorkOrderStatus.SCHEDULED, WorkOrderStatus.IN_PROGRESS);

    private static final Set<WorkOrderStatus> COMPLETED_STATUSES =
            EnumSet.of(WorkOrderStatus.COMPLETED, WorkOrderStatus.CLOSED);

    private static final Set<ServiceRequestStatus> OPEN_SERVICE_REQUEST_STATUSES =
            EnumSet.of(ServiceRequestStatus.NEW, ServiceRequestStatus.UNDER_REVIEW, ServiceRequestStatus.APPROVED);

    private static final Set<MaterialRequisitionStatus> ACTIVE_MR_STATUSES =
            EnumSet.of(MaterialRequisitionStatus.SUBMITTED, MaterialRequisitionStatus.APPROVED);

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
    private static final String COMPARISON_PERIOD = "last_week";

    private final WorkOrderRepository workOrderRepository;
    private final ServiceMaintenanceRepository serviceMaintenanceRepository;
    private final MaterialRequisitionRepository materialRequisitionRepository;
    private final TechnicianRepository technicianRepository;
    private final TechnicianLeaveRepository technicianLeaveRepository;
    private final TechnicianHolidayRepository technicianHolidayRepository;

    public DashboardResponse getDashboard() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekStart = now.minusWeeks(1);
        LocalDateTime prevWeekStart = now.minusWeeks(2);

        Instant nowInstant = Instant.now();
        Instant weekAgoInstant = nowInstant.minus(7, ChronoUnit.DAYS);
        Instant twoWeeksAgoInstant = nowInstant.minus(14, ChronoUnit.DAYS);

        SummaryMetric openServiceRequests = buildMetric(
                serviceMaintenanceRepository.countByDeletedFalseAndStatusIn(OPEN_SERVICE_REQUEST_STATUSES),
                serviceMaintenanceRepository.countByDeletedFalseAndStatusInAndRequestDateBetween(OPEN_SERVICE_REQUEST_STATUSES, weekStart, now),
                serviceMaintenanceRepository.countByDeletedFalseAndStatusInAndRequestDateBetween(OPEN_SERVICE_REQUEST_STATUSES, prevWeekStart, weekStart)
        );

        SummaryMetric activeWorkOrders = buildMetric(
                workOrderRepository.countByStatusInAndDeletedFalse(ACTIVE_WORK_ORDER_STATUSES),
                workOrderRepository.countByStatusInAndCreatedAtBetweenAndDeletedFalse(ACTIVE_WORK_ORDER_STATUSES, weekStart, now),
                workOrderRepository.countByStatusInAndCreatedAtBetweenAndDeletedFalse(ACTIVE_WORK_ORDER_STATUSES, prevWeekStart, weekStart)
        );

        SummaryMetric activeMaterialRequisitions = buildMetric(
                materialRequisitionRepository.countByStatusIn(ACTIVE_MR_STATUSES),
                materialRequisitionRepository.countByStatusInAndCreatedAtBetween(ACTIVE_MR_STATUSES, weekAgoInstant, nowInstant),
                materialRequisitionRepository.countByStatusInAndCreatedAtBetween(ACTIVE_MR_STATUSES, twoWeeksAgoInstant, weekAgoInstant)
        );

        SummaryMetric criticalAssetsDown = buildMetric(
                workOrderRepository.countActiveCriticalAssetsDown(AssetCriticality.CRITICAL, ACTIVE_WORK_ORDER_STATUSES),
                workOrderRepository.countByAsset_CriticalityAndStatusInAndDowntimeStartBetweenAndDeletedFalse(
                        AssetCriticality.CRITICAL, ACTIVE_WORK_ORDER_STATUSES, weekStart, now),
                workOrderRepository.countByAsset_CriticalityAndStatusInAndDowntimeStartBetweenAndDeletedFalse(
                        AssetCriticality.CRITICAL, ACTIVE_WORK_ORDER_STATUSES, prevWeekStart, weekStart)
        );

        SummaryMetrics summaryMetrics = SummaryMetrics.builder()
                .openServiceRequests(openServiceRequests)
                .activeWorkOrders(activeWorkOrders)
                .activeMaterialRequisitions(activeMaterialRequisitions)
                .criticalAssetsDown(criticalAssetsDown)
                .build();

        WorkOrderStatusSummary workOrdersByStatus = WorkOrderStatusSummary.builder()
                .newCount(workOrderRepository.countByStatusAndDeletedFalse(WorkOrderStatus.NEW))
                .inProgress(workOrderRepository.countByStatusAndDeletedFalse(WorkOrderStatus.IN_PROGRESS))
                .completed(workOrderRepository.countByStatusInAndDeletedFalse(COMPLETED_STATUSES))
                .total(workOrderRepository.countByDeletedFalse())
                .build();

        MaintenanceCostSummary maintenanceCostSummary = buildMaintenanceCostSummary();
        List<RecentWorkOrderDto> recentWorkOrders = mapRecentWorkOrders();
        List<NewServiceRequestDto> newServiceRequests = mapNewServiceRequests();
        long requestsNotAcceptedCount = workOrderRepository.countByStatusAndDeletedFalse(WorkOrderStatus.NEW);
        LocalDate today = LocalDate.now();
        MaintenanceListSection upcomingMaintenance = buildUpcomingMaintenance(today);
        MaintenanceListSection pastDueMaintenance = buildPastDueMaintenance(today);

        DashboardMetadata metadata = DashboardMetadata.builder()
                .generatedAt(Instant.now().toString())
                .dataFreshness("real_time")
                .build();

        return DashboardResponse.builder()
                .summaryMetrics(summaryMetrics)
                .workOrdersByStatus(workOrdersByStatus)
                .maintenanceCostSummary(maintenanceCostSummary)
                .recentWorkOrders(recentWorkOrders)
                .newServiceRequests(newServiceRequests)
                .requestsNotAcceptedCount(requestsNotAcceptedCount)
                .upcomingMaintenance(upcomingMaintenance)
                .pastDueMaintenance(pastDueMaintenance)
                .metadata(metadata)
                .build();
    }

    public TechnicianDashboardResponse getTechnicianDashboard(Integer limit) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        long totalTechnicians = technicianRepository.count();

        boolean isHolidayToday = technicianHolidayRepository.existsByHolidayDate(today);

        // technicians busy today (direct or via team)
        List<Long> busyIds = workOrderRepository.findDistinctTechnicianIdsWithBookings(
                start,
                end,
                EnumSet.of(WorkOrderStatus.SCHEDULED, WorkOrderStatus.IN_PROGRESS)
        );

        long onLeave = technicianLeaveRepository.countTechniciansOnLeave(today);
        long availableToday = isHolidayToday ? 0 : totalTechnicians - busyIds.size() - onLeave;

        long totalWorkOrders = workOrderRepository.countByDeletedFalse();

        int resolvedLimit = resolveLimit(limit);
        List<TechnicianActivityDto> activities = buildRecentTechnicianActivities(resolvedLimit);

        return TechnicianDashboardResponse.builder()
                .totalTechnicians(totalTechnicians)
                .availableToday(Math.max(availableToday, 0))
                .onLeave(onLeave)
                .workOrders(totalWorkOrders)
                .recentActivities(activities)
                .build();
    }

    private List<TechnicianActivityDto> buildRecentTechnicianActivities(int limit) {
        return workOrderRepository.findByDeletedFalseOrderByUpdatedAtDesc(org.springframework.data.domain.PageRequest.of(0, limit)).stream()
                .map(wo -> {
                    String technicianName = wo.getAssignedTechnician() != null
                            ? wo.getAssignedTechnician().getFullName()
                            : wo.getAssignedTeam() != null ? wo.getAssignedTeam().getTeamName() : "Unassigned";

                    String activity = switch (wo.getStatus()) {
                        case COMPLETED -> "Completed Work Order #" + wo.getWorkOrderId();
                        case IN_PROGRESS -> "Started Work Order #" + wo.getWorkOrderId();
                        case SCHEDULED -> "Scheduled Work Order #" + wo.getWorkOrderId();
                        default -> "Updated Work Order #" + wo.getWorkOrderId();
                    };

                    String timeAgo = formatTimeAgo(wo.getUpdatedAt());

                    return TechnicianActivityDto.builder()
                            .technicianName(technicianName)
                            .activity(activity)
                            .timeAgo(timeAgo)
                            .build();
                })
                .toList();
    }

    private int resolveLimit(Integer limit) {
        if (limit == null) return 5;
        if (limit < 1) return 1;
        return Math.min(limit, 50);
    }

    private String formatTimeAgo(LocalDateTime time) {
        if (time == null) return "just now";
        long minutes = ChronoUnit.MINUTES.between(time, LocalDateTime.now());
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + " mins ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + (hours == 1 ? " hour ago" : " hours ago");
        long days = hours / 24;
        return days + (days == 1 ? " day ago" : " days ago");
    }

    private SummaryMetric buildMetric(long value, long currentWindow, long previousWindow) {
        Double change = calculateChangePercentage(currentWindow, previousWindow);
        Double absoluteChange = change != null ? Math.abs(change) : null;
        String direction = change == null ? null : change >= 0 ? "up" : "down";

        return SummaryMetric.builder()
                .count(value)
                .changePercentage(absoluteChange)
                .changeDirection(direction)
                .comparisonPeriod(COMPARISON_PERIOD)
                .build();
    }

    private Double calculateChangePercentage(long current, long previous) {
        if (previous == 0) {
            return null;
        }
        double diff = current - previous;
        return (diff / previous) * 100d;
    }

    private MaintenanceListSection buildUpcomingMaintenance(LocalDate today) {
        List<WorkOrder> upcoming = workOrderRepository
                .findTop5ByTargetCompletionDateAfterAndStatusInAndDeletedFalseOrderByTargetCompletionDateAsc(
                        today, ACTIVE_WORK_ORDER_STATUSES);

        long count = workOrderRepository.countByTargetCompletionDateAfterAndStatusInAndDeletedFalse(
                today, ACTIVE_WORK_ORDER_STATUSES);

        return MaintenanceListSection.builder()
                .count(count)
                .items(mapMaintenanceItems(upcoming))
                .build();
    }

    private MaintenanceListSection buildPastDueMaintenance(LocalDate today) {
        List<WorkOrder> pastDue = workOrderRepository
                .findTop5ByTargetCompletionDateBeforeAndStatusInAndDeletedFalseOrderByTargetCompletionDateAsc(
                        today, ACTIVE_WORK_ORDER_STATUSES);

        long count = workOrderRepository.countByTargetCompletionDateBeforeAndStatusInAndDeletedFalse(
                today, ACTIVE_WORK_ORDER_STATUSES);

        return MaintenanceListSection.builder()
                .count(count)
                .items(mapMaintenanceItems(pastDue))
                .build();
    }

    private MaintenanceCostSummary buildMaintenanceCostSummary() {
        LocalDate firstMonth = LocalDate.now().withDayOfMonth(1).minusMonths(5);
        List<MaintenanceCostPoint> points = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            LocalDate monthStart = firstMonth.plusMonths(i);
            LocalDateTime start = monthStart.atStartOfDay();
            LocalDateTime end = monthStart.plusMonths(1).atStartOfDay();

            BigDecimal monthlyCost = workOrderRepository.findByDeletedFalseAndCreatedAtBetween(start, end).stream()
                    .map(this::resolveWorkOrderCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal costInThousands = monthlyCost.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);

            points.add(MaintenanceCostPoint.builder()
                    .month(monthStart.format(MONTH_FORMATTER))
                    .cost(costInThousands)
                    .currency("USD")
                    .unit("thousands")
                    .build());
        }

        return MaintenanceCostSummary.builder()
                .period("last_6_months")
                .data(points)
                .build();
    }

    private List<MaintenanceItemDto> mapMaintenanceItems(List<WorkOrder> workOrders) {
        return workOrders.stream()
                .map(wo -> MaintenanceItemDto.builder()
                        .workOrderDbId(wo.getId())
                        .workOrderId(wo.getWorkOrderId())
                        .title(wo.getWoTitle())
                        .asset(wo.getAsset() != null ? wo.getAsset().getAssetName() : null)
                        .dueDate(wo.getTargetCompletionDate())
                        .priority(wo.getPriority())
                        .status(wo.getStatus())
                        .workType(wo.getWorkType())
                        .preventive(isPreventive(wo))
                        .build())
                .toList();
    }

    private boolean isPreventive(WorkOrder workOrder) {
        return workOrder.getPmPlan() != null || workOrder.getWorkType() == WorkType.PREVENTIVE;
    }

    private BigDecimal resolveWorkOrderCost(WorkOrder wo) {
        if (wo.getActualTotalCost() != null) {
            return wo.getActualTotalCost();
        }
        if (wo.getEstimatedTotalCost() != null) {
            return wo.getEstimatedTotalCost();
        }

        BigDecimal actualLabor = wo.getActualLaborCost() != null ? wo.getActualLaborCost() : BigDecimal.ZERO;
        BigDecimal actualMaterial = wo.getActualMaterialCost() != null ? wo.getActualMaterialCost() : BigDecimal.ZERO;
        BigDecimal estimatedMaterial = wo.getEstimatedMaterialCost() != null ? wo.getEstimatedMaterialCost() : BigDecimal.ZERO;

        return actualLabor.add(actualMaterial).add(estimatedMaterial);
    }

    private List<RecentWorkOrderDto> mapRecentWorkOrders() {
        return workOrderRepository.findTop5ByDeletedFalseOrderByCreatedAtDesc().stream()
                .map(wo -> RecentWorkOrderDto.builder()
                        .workOrderDbId(wo.getId())
                        .workOrderId(wo.getWorkOrderId())
                        .title(wo.getWoTitle())
                        .asset(wo.getAsset() != null ? wo.getAsset().getAssetName() : null)
                        .technician(wo.getAssignedTechnician() != null ? wo.getAssignedTechnician().getFullName() : null)
                        .dueDate(wo.getTargetCompletionDate())
                        .priority(wo.getPriority())
                        .status(wo.getStatus())
                        .build())
                .toList();
    }

    private List<NewServiceRequestDto> mapNewServiceRequests() {
        return serviceMaintenanceRepository.findByDeletedFalseAndStatusOrderByRequestDateDesc(ServiceRequestStatus.NEW).stream()
                .map(sr -> NewServiceRequestDto.builder()
                        .serviceRequestDbId(sr.getId())
                        .serviceRequestId(sr.getRequestId())
                        .title(sr.getShortTitle())
                        .asset(sr.getAsset() != null ? sr.getAsset().getAssetName() : null)
                        .requesterName(sr.getRequesterName())
                        .requestDate(sr.getRequestDate())
                        .priority(sr.getPriority())
                        .status(sr.getStatus())
                        .build())
                .toList();
    }

}
