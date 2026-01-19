package com.example.eam.Dashboard.Service;

import com.example.eam.Dashboard.Dto.*;
import com.example.eam.Enum.WorkOrderStatus;
import com.example.eam.WorkOrder.Repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final Set<WorkOrderStatus> ACTIVE_WORK_ORDER_STATUSES =
            EnumSet.of(WorkOrderStatus.NEW, WorkOrderStatus.APPROVED, WorkOrderStatus.SCHEDULED, WorkOrderStatus.IN_PROGRESS);

    private static final Set<WorkOrderStatus> COMPLETED_STATUSES =
            EnumSet.of(WorkOrderStatus.COMPLETED, WorkOrderStatus.CLOSED);

    private static final Set<WorkOrderStatus> PENDING_STATUSES =
            EnumSet.of(WorkOrderStatus.NEW, WorkOrderStatus.APPROVED, WorkOrderStatus.SCHEDULED);

    private final WorkOrderRepository workOrderRepository;

    public DashboardResponse getDashboard() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekStart = now.minusWeeks(1);
        LocalDateTime prevWeekStart = now.minusWeeks(2);

        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);
        LocalDate twoWeeksAgo = today.minusDays(14);

        MetricCard activeWorkOrders = buildMetric(
                workOrderRepository.countByStatusInAndDeletedFalse(ACTIVE_WORK_ORDER_STATUSES),
                workOrderRepository.countByStatusInAndCreatedAtBetweenAndDeletedFalse(ACTIVE_WORK_ORDER_STATUSES, weekStart, now),
                workOrderRepository.countByStatusInAndCreatedAtBetweenAndDeletedFalse(ACTIVE_WORK_ORDER_STATUSES, prevWeekStart, weekStart)
        );

        MetricCard inProgressWorkOrders = MetricCard.builder()
                .value(workOrderRepository.countByStatusAndDeletedFalse(WorkOrderStatus.IN_PROGRESS))
                .changePercentage(null)
                .trendUp(null)
                .build();

        MetricCard overdueTasks = buildMetric(
                workOrderRepository.countByTargetCompletionDateBeforeAndStatusInAndDeletedFalse(today, ACTIVE_WORK_ORDER_STATUSES),
                workOrderRepository.countByTargetCompletionDateBetweenAndStatusInAndDeletedFalse(weekAgo, today, ACTIVE_WORK_ORDER_STATUSES),
                workOrderRepository.countByTargetCompletionDateBetweenAndStatusInAndDeletedFalse(twoWeeksAgo, weekAgo, ACTIVE_WORK_ORDER_STATUSES)
        );

        WorkOrderStatusBreakdown workOrdersByStatus = WorkOrderStatusBreakdown.builder()
                .completed(workOrderRepository.countByStatusInAndDeletedFalse(COMPLETED_STATUSES))
                .inProgress(workOrderRepository.countByStatusAndDeletedFalse(WorkOrderStatus.IN_PROGRESS))
                .pending(workOrderRepository.countByStatusInAndDeletedFalse(PENDING_STATUSES))
                .build();

        List<RecentWorkOrderDto> recentWorkOrders = mapRecentWorkOrders();

        return DashboardResponse.builder()
                .activeWorkOrders(activeWorkOrders)
                .overdueTasks(overdueTasks)
                .inProgressWorkOrders(inProgressWorkOrders)
                .workOrdersByStatus(workOrdersByStatus)
                .recentWorkOrders(recentWorkOrders)
                .build();
    }

    private MetricCard buildMetric(long value, long currentWindow, long previousWindow) {
        Double change = calculateChangePercentage(currentWindow, previousWindow);
        Boolean trendUp = change != null ? change >= 0 : null;
        return MetricCard.builder()
                .value(value)
                .changePercentage(change)
                .trendUp(trendUp)
                .build();
    }

    private Double calculateChangePercentage(long current, long previous) {
        if (previous == 0) {
            return null;
        }
        double diff = current - previous;
        return (diff / previous) * 100d;
    }

    private List<RecentWorkOrderDto> mapRecentWorkOrders() {
        return workOrderRepository.findTop5ByDeletedFalseOrderByCreatedAtDesc().stream()
                .map(wo -> RecentWorkOrderDto.builder()
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
}
