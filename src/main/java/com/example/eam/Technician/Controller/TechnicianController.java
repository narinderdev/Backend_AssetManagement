package com.example.eam.Technician.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Technician.Dto.*;
import com.example.eam.Technician.Service.TechnicianService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/technicians")
@RequiredArgsConstructor
@Validated
public class TechnicianController {

    private final TechnicianService technicianService;

    @PostMapping
    public ResponseEntity<ApiResponse<TechnicianDetailsResponse>> create(@Valid @RequestBody TechnicianCreateRequest request) {
        TechnicianDetailsResponse data = technicianService.createTechnician(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successResponse(HttpStatus.CREATED.value(), "Technician created successfully", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TechnicianDetailsResponse>> get(@PathVariable Long id) {
        TechnicianDetailsResponse data = technicianService.getTechnician(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Technician fetched successfully", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<TechnicianListResponse>> list(Pageable pageable) {
        TechnicianListResponse data = technicianService.listTechnicians(pageable);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Technicians fetched successfully", data));
    }

    @GetMapping("/{id}/availability/monthly")
    public ResponseEntity<ApiResponse<java.util.List<DailyAvailabilityDto>>> monthlyAvailability(
            @PathVariable Long id,
            @RequestParam(name = "days", required = false) Integer daysAhead
    ) {
        java.util.List<DailyAvailabilityDto> data = technicianService.getMonthlyAvailability(id, daysAhead);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Technician monthly availability fetched", data));
    }

    @PostMapping("/{id}/leaves")
    public ResponseEntity<ApiResponse<TechnicianLeaveResponse>> applyLeave(@PathVariable Long id,
                                                                           @Valid @RequestBody TechnicianLeaveRequest request) {
        TechnicianLeaveResponse data = technicianService.applyLeave(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successResponse(HttpStatus.CREATED.value(), "Technician leave applied successfully", data));
    }

    @GetMapping("/{id}/leaves")
    public ResponseEntity<ApiResponse<TechnicianLeaveListResponse>> listLeaves(@PathVariable Long id,
                                                                               @RequestParam(value = "startDate", required = false) java.time.LocalDate startDate,
                                                                               @RequestParam(value = "endDate", required = false) java.time.LocalDate endDate) {
        TechnicianLeaveListResponse data = technicianService.listLeaves(id, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Technician leaves fetched successfully", data));
    }

    @GetMapping("/{id}/leaves/{leaveId}")
    public ResponseEntity<ApiResponse<TechnicianLeaveResponse>> getLeave(@PathVariable Long id,
                                                                          @PathVariable Long leaveId) {
        TechnicianLeaveResponse data = technicianService.getLeave(id, leaveId);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Technician leave fetched successfully", data));
    }

    @PatchMapping("/{id}/leaves/{leaveId}")
    public ResponseEntity<ApiResponse<TechnicianLeaveResponse>> patchLeave(@PathVariable Long id,
                                                                            @PathVariable Long leaveId,
                                                                            @Valid @RequestBody TechnicianLeavePatchRequest request) {
        TechnicianLeaveResponse data = technicianService.patchLeave(id, leaveId, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Technician leave updated successfully", data));
    }

    @DeleteMapping("/{id}/leaves/{leaveId}")
    public ResponseEntity<ApiResponse<Void>> deleteLeave(@PathVariable Long id,
                                                         @PathVariable Long leaveId) {
        technicianService.deleteLeave(id, leaveId);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Technician leave deleted successfully", null));
    }

    @GetMapping("/leaves")
    public ResponseEntity<ApiResponse<TechnicianLeaveListResponse>> listAllLeaves(
            @RequestParam(value = "startDate", required = false) java.time.LocalDate startDate,
            @RequestParam(value = "endDate", required = false) java.time.LocalDate endDate) {
        TechnicianLeaveListResponse data = technicianService.listAllLeaves(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "All technician leaves fetched successfully", data));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<TechnicianDetailsResponse>> patch(@PathVariable Long id,
                                                                        @RequestBody TechnicianPatchRequest request) {
        TechnicianDetailsResponse data = technicianService.patchTechnician(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Technician updated successfully", data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        technicianService.deleteTechnician(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Technician deleted successfully", null));
    }
}
