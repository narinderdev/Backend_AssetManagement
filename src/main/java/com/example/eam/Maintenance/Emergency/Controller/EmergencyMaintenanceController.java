package com.example.eam.Maintenance.Emergency.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Maintenance.Emergency.Dto.EmergencyWorkOrderRequest;
import com.example.eam.Maintenance.Emergency.Service.EmergencyMaintenanceService;
import com.example.eam.WorkOrder.Entity.WorkOrder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/maintenance/emergency")
@RequiredArgsConstructor
public class EmergencyMaintenanceController {

    private final EmergencyMaintenanceService service;

    @PostMapping
    public ResponseEntity<ApiResponse<WorkOrder>> create(@Valid @RequestBody EmergencyWorkOrderRequest req) {
        WorkOrder wo = service.createEmergencyWo(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successResponse(HttpStatus.CREATED.value(), "Emergency work order created", wo));
    }
}
