package com.example.eam.Reports.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Enum.WorkOrderStatus;
import com.example.eam.WorkOrder.Dto.WorkOrderBudgetSummaryResponse;
import com.example.eam.WorkOrder.Dto.WorkOrderListResponse;
import com.example.eam.WorkOrder.Service.WorkOrderBudgetService;
import com.example.eam.WorkOrder.Service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/reports/work-orders")
@RequiredArgsConstructor
public class WorkOrderReportController {

    private final WorkOrderService workOrderService;
    private final WorkOrderBudgetService workOrderBudgetService;

    @GetMapping
    public ResponseEntity<ApiResponse<WorkOrderListResponse>> list(
            @RequestParam(value = "statuses", required = false) List<WorkOrderStatus> statuses,
            @RequestParam(value = "status", required = false) WorkOrderStatus status,
            Pageable pageable
    ) {
        Set<WorkOrderStatus> filterStatuses;
        if (statuses != null && !statuses.isEmpty()) {
            filterStatuses = EnumSet.copyOf(statuses);
        } else if (status != null) {
            filterStatuses = EnumSet.of(status);
        } else {
            filterStatuses = EnumSet.of(
                    WorkOrderStatus.NEW,
                    WorkOrderStatus.APPROVED,
                    WorkOrderStatus.REJECTED,
                    WorkOrderStatus.SCHEDULED,
                    WorkOrderStatus.IN_PROGRESS,
                    WorkOrderStatus.COMPLETED,
                    WorkOrderStatus.CLOSED
            );
        }

        WorkOrderListResponse data = workOrderService.listWorkOrdersByStatus(filterStatuses, pageable);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Work order report fetched", data));
    }

    @GetMapping("/budget")
    public ResponseEntity<ApiResponse<WorkOrderBudgetSummaryResponse>> budgetSummary(
            @RequestParam(value = "assetId", required = false) String assetId,
            @RequestParam(value = "workOrderId", required = false) String workOrderId,
            @RequestParam(value = "period", required = false, defaultValue = "THIS_MONTH") String period
    ) {
        WorkOrderBudgetSummaryResponse data = workOrderBudgetService.getBudgetSummary(
                assetId,
                workOrderId,
                period
        );
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Work order budget summary fetched",
                data
        ));
    }
}
