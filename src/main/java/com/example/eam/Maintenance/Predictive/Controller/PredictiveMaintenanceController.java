package com.example.eam.Maintenance.Predictive.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Maintenance.Predictive.Dto.AssetThresholdListResponse;
import com.example.eam.Maintenance.Predictive.Dto.AssetThresholdRequest;
import com.example.eam.Maintenance.Predictive.Dto.AssetThresholdResponse;
import com.example.eam.Maintenance.Predictive.Dto.MeterReadingRequest;
import com.example.eam.Maintenance.Predictive.Service.PredictiveMaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/maintenance/predictive")
@RequiredArgsConstructor
public class PredictiveMaintenanceController {

    private final PredictiveMaintenanceService service;

    @PostMapping("/threshold")
    public ResponseEntity<ApiResponse<AssetThresholdResponse>> createThreshold(@Valid @RequestBody AssetThresholdRequest req) {
        AssetThresholdResponse saved = service.createThreshold(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successResponse(HttpStatus.CREATED.value(), "Threshold created", saved));
    }

    @PutMapping("/threshold/{id}")
    public ResponseEntity<ApiResponse<AssetThresholdResponse>> updateThreshold(@PathVariable Long id,
                                                                               @Valid @RequestBody AssetThresholdRequest req) {
        AssetThresholdResponse updated = service.updateThreshold(id, req);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Threshold updated", updated));
    }

    @GetMapping("/threshold")
    public ResponseEntity<ApiResponse<AssetThresholdListResponse>> listThresholds(Pageable pageable) {
        AssetThresholdListResponse data = service.listThresholds(pageable);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Thresholds fetched", data));
    }

    @GetMapping("/threshold/{id}")
    public ResponseEntity<ApiResponse<AssetThresholdResponse>> getThreshold(@PathVariable Long id) {
        AssetThresholdResponse data = service.getThreshold(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Threshold fetched", data));
    }

    @DeleteMapping("/threshold/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteThreshold(@PathVariable Long id) {
        service.deleteThreshold(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Threshold deleted", null));
    }

    @PostMapping("/meter-reading")
    public ResponseEntity<ApiResponse<Void>> recordReading(@Valid @RequestBody MeterReadingRequest req) {
        service.recordMeterReading(req);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Meter reading processed", null));
    }
}
