package com.example.eam.Maintenance.Predictive.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Maintenance.Predictive.Dto.AssetThresholdRequest;
import com.example.eam.Maintenance.Predictive.Dto.MeterReadingRequest;
import com.example.eam.Maintenance.Predictive.Entity.AssetThreshold;
import com.example.eam.Maintenance.Predictive.Service.PredictiveMaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/maintenance/predictive")
@RequiredArgsConstructor
public class PredictiveMaintenanceController {

    private final PredictiveMaintenanceService service;

    @PostMapping("/threshold")
    public ResponseEntity<ApiResponse<AssetThreshold>> upsertThreshold(@Valid @RequestBody AssetThresholdRequest req) {
        AssetThreshold saved = service.upsertThreshold(req);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Threshold saved", saved));
    }

    @PostMapping("/meter-reading")
    public ResponseEntity<ApiResponse<Void>> recordReading(@Valid @RequestBody MeterReadingRequest req) {
        service.recordMeterReading(req);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Meter reading processed", null));
    }
}
