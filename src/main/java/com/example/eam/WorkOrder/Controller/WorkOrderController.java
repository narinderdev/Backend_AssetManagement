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

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<WorkOrderDetailsResponse>> reject(
            @PathVariable Long id,
            @Valid @RequestBody WorkOrderRejectRequest request
    ) {
        WorkOrderDetailsResponse data = workOrderService.rejectWorkOrder(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Work order rejected", data));
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
            @RequestBody(required = false) WorkOrderInProgressRequest request
    ) {
        WorkOrderDetailsResponse data = workOrderService.markInProgress(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Work order in progress", data));
    }

    @PostMapping("/{id}/check-in")
    public ResponseEntity<ApiResponse<WorkOrderDetailsResponse>> checkIn(
            @PathVariable Long id,
            @Valid @RequestBody WorkOrderCheckInRequest request
    ) {
        WorkOrderDetailsResponse data = workOrderService.checkIn(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Work order check-in recorded", data));
    }

    @PostMapping("/{id}/check-out")
    public ResponseEntity<ApiResponse<WorkOrderDetailsResponse>> checkOut(
            @PathVariable Long id,
            @Valid @RequestBody WorkOrderCheckOutRequest request
    ) {
        WorkOrderDetailsResponse data = workOrderService.checkOut(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Work order check-out recorded", data));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<ApiResponse<WorkOrderDetailsResponse>> pause(
            @PathVariable Long id,
            @RequestBody(required = false) WorkOrderPauseRequest request
    ) {
        WorkOrderDetailsResponse data = workOrderService.pause(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Work order paused", data));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<ApiResponse<WorkOrderDetailsResponse>> resume(
            @PathVariable Long id,
            @RequestBody(required = false) WorkOrderResumeRequest request
    ) {
        WorkOrderDetailsResponse data = workOrderService.resume(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Work order resumed", data));
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

    @GetMapping("/assigned-to-technician")
    public ResponseEntity<ApiResponse<WorkOrderListResponse>> listForTechnician(
            @RequestParam Long technicianId,
            Pageable pageable
    ) {
        WorkOrderListResponse data = workOrderService.listScheduledForTechnician(technicianId, pageable);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Technician work orders fetched successfully", data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        workOrderService.deleteWorkOrder(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Work order deleted successfully", null));
    }
}
