package com.example.eam.Maintenance.Emergency.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Maintenance.Emergency.Dto.EmergencyIncidentListResponse;
import com.example.eam.Maintenance.Emergency.Dto.EmergencyIncidentResponse;
import com.example.eam.Maintenance.Emergency.Dto.EmergencyWorkOrderRequest;
import com.example.eam.Maintenance.Emergency.Service.EmergencyMaintenanceService;
import com.example.eam.WorkOrder.Dto.WorkOrderDetailsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/maintenance/emergency")
@RequiredArgsConstructor
public class EmergencyMaintenanceController {

    private final EmergencyMaintenanceService service;

    @PostMapping
    public ResponseEntity<ApiResponse<WorkOrderDetailsResponse>> create(@Valid @RequestBody EmergencyWorkOrderRequest req) {
        WorkOrderDetailsResponse wo = service.createEmergencyWo(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successResponse(HttpStatus.CREATED.value(), "Emergency work order created", wo));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<EmergencyIncidentListResponse>> list(Pageable pageable) {
        EmergencyIncidentListResponse data = service.listIncidents(pageable);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Emergency incidents fetched successfully", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmergencyIncidentResponse>> get(@PathVariable Long id) {
        EmergencyIncidentResponse data = service.getIncident(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Emergency incident fetched successfully", data));
    }
}
