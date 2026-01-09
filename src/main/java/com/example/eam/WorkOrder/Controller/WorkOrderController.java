package com.example.eam.WorkOrder.Controller;


import com.example.eam.Common.ApiResponse;
import com.example.eam.WorkOrder.Dto.*;
import com.example.eam.WorkOrder.Service.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
@Validated
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @PostMapping
    public ResponseEntity<ApiResponse<WorkOrderDetailsResponse>> create(@Valid @RequestBody WorkOrderCreateRequest request) {
        WorkOrderDetailsResponse data = workOrderService.createWorkOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successResponse(HttpStatus.CREATED.value(), "Work order created successfully", data));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkOrderDetailsResponse>> patch(@PathVariable Long id,
                                                                      @RequestBody WorkOrderPatchRequest request) {
        WorkOrderDetailsResponse data = workOrderService.patchWorkOrder(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Work order updated successfully", data));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<WorkOrderDetailsResponse>> approve(
            @PathVariable Long id,
            @Valid @RequestBody WorkOrderApproveRequest request
    ) {
        WorkOrderDetailsResponse data = workOrderService.approveWorkOrder(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Work order approved", data));
    }

    @PostMapping("/{id}/schedule")
    public ResponseEntity<ApiResponse<WorkOrderDetailsResponse>> schedule(
            @PathVariable Long id,
            @Valid @RequestBody WorkOrderScheduleRequest request
    ) {
        WorkOrderDetailsResponse data = workOrderService.scheduleWorkOrder(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Work order scheduled", data));
    }

    @PostMapping("/{id}/in-progress")
    public ResponseEntity<ApiResponse<WorkOrderDetailsResponse>> markInProgress(
            @PathVariable Long id,
            @RequestBody WorkOrderInProgressRequest request
    ) {
        WorkOrderDetailsResponse data = workOrderService.markInProgress(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Work order in progress", data));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<WorkOrderDetailsResponse>> complete(@PathVariable Long id,
                                                                          @Valid @RequestBody WorkOrderCompletionRequest request) {
        WorkOrderDetailsResponse data = workOrderService.recordTechnicianCompletion(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Work order marked as completed", data));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<WorkOrderDetailsResponse>> close(@PathVariable Long id,
                                                                       @RequestBody WorkOrderCloseRequest request) {
        WorkOrderDetailsResponse data = workOrderService.closeWorkOrder(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Work order closed successfully", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkOrderDetailsResponse>> get(@PathVariable Long id) {
        WorkOrderDetailsResponse data = workOrderService.getWorkOrderDetails(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Work order details fetched successfully", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<WorkOrderListResponse>> list(Pageable pageable) {
        WorkOrderListResponse data = workOrderService.listWorkOrders(pageable);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Work orders fetched successfully", data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        workOrderService.deleteWorkOrder(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Work order deleted successfully", null));
    }
}
