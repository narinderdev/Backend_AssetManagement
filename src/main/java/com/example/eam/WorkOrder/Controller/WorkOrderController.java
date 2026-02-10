package com.example.eam.WorkOrder.Controller;


import com.example.eam.Common.ApiResponse;
import com.example.eam.WorkOrder.Dto.*;
import com.example.eam.WorkOrder.Service.WorkOrderService;
import com.example.eam.WorkOrder.Service.WorkOrderInvoiceService;
import com.example.eam.WorkRequestType.Dto.WorkRequestTypeResponse;
import com.example.eam.WorkRequestType.Service.WorkRequestTypeService;
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
    private final WorkOrderInvoiceService workOrderInvoiceService;
    private final WorkRequestTypeService workRequestTypeService;

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

    @PostMapping("/availability/technician/{technicianId}")
    public ResponseEntity<ApiResponse<java.util.List<AvailabilitySlotResponse>>> technicianAvailability(
            @PathVariable Long technicianId,
            @Valid @RequestBody WorkOrderAvailabilityRequest request
    ) {
        java.util.List<AvailabilitySlotResponse> data = workOrderService.getTechnicianAvailability(technicianId, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Technician availability slots fetched", data));
    }

    @PostMapping("/availability/team/{teamId}")
    public ResponseEntity<ApiResponse<java.util.List<AvailabilitySlotResponse>>> teamAvailability(
            @PathVariable Long teamId,
            @Valid @RequestBody WorkOrderAvailabilityRequest request
    ) {
        java.util.List<AvailabilitySlotResponse> data = workOrderService.getTeamAvailability(teamId, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Team availability slots fetched", data));
    }

    @PostMapping("/availability/time-slots")
    public ResponseEntity<ApiResponse<java.util.List<AvailabilitySlotResponse>>> availabilityTimeSlots(
            @Valid @RequestBody WorkOrderAvailabilityRequest request
    ) {
        java.util.List<AvailabilitySlotResponse> data = workOrderService.getAvailabilityTimeSlots(request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Availability time slots fetched", data));
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

    @PostMapping("/{id}/team/check-in")
    public ResponseEntity<ApiResponse<WorkOrderDetailsResponse>> teamCheckIn(
            @PathVariable Long id,
            @Valid @RequestBody WorkOrderTeamCheckInRequest request
    ) {
        WorkOrderDetailsResponse data = workOrderService.teamCheckIn(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Team check-in recorded", data));
    }

    @PostMapping("/{id}/team/check-out")
    public ResponseEntity<ApiResponse<WorkOrderDetailsResponse>> teamCheckOut(
            @PathVariable Long id,
            @Valid @RequestBody WorkOrderTeamCheckOutRequest request
    ) {
        WorkOrderDetailsResponse data = workOrderService.teamCheckOut(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Team check-out recorded", data));
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

    @PostMapping("/{id}/team/pause")
    public ResponseEntity<ApiResponse<WorkOrderDetailsResponse>> teamPause(
            @PathVariable Long id,
            @Valid @RequestBody WorkOrderTeamPauseRequest request
    ) {
        WorkOrderDetailsResponse data = workOrderService.teamPause(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Team pause recorded", data));
    }

    @PostMapping("/{id}/team/resume")
    public ResponseEntity<ApiResponse<WorkOrderDetailsResponse>> teamResume(
            @PathVariable Long id,
            @Valid @RequestBody WorkOrderTeamResumeRequest request
    ) {
        WorkOrderDetailsResponse data = workOrderService.teamResume(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Team resume recorded", data));
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

    @PostMapping(value = "/{id}/invoice", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generateInvoice(@PathVariable Long id,
                                                  @Valid @RequestBody WorkOrderInvoiceRequest request) {
        var result = workOrderInvoiceService.generateInvoice(id, request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(result.fileName()).build());
        headers.add("X-Invoice-Id", result.invoiceId());
        return new ResponseEntity<>(result.pdfBytes(), headers, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkOrderDetailsResponse>> get(@PathVariable Long id) {
        WorkOrderDetailsResponse data = workOrderService.getWorkOrderDetails(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Work order details fetched successfully", data));
    }

    @GetMapping("/work-request-types")
    public ResponseEntity<ApiResponse<java.util.List<WorkRequestTypeResponse>>> listWorkRequestTypes() {
        java.util.List<WorkRequestTypeResponse> types = workRequestTypeService.listTypes();
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(),
                "Work request types fetched successfully", types));
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
