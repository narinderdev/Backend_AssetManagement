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
