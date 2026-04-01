package com.example.eam.Maintenance.Iot.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Maintenance.Iot.Dto.*;
import com.example.eam.Maintenance.Iot.Enum.IotAlertSeverity;
import com.example.eam.Maintenance.Iot.Service.IotIntegrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class IotIntegrationController {

    private final IotIntegrationService iotIntegrationService;

    @PostMapping("/api/maintenance/iot/devices")
    public ResponseEntity<ApiResponse<IotDeviceResponse>> createDevice(@Valid @RequestBody IotDeviceCreateRequest req) {
        IotDeviceResponse data = iotIntegrationService.createDevice(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successResponse(HttpStatus.CREATED.value(), "IoT device registered", data));
    }

    @PutMapping("/api/maintenance/iot/devices/{id}")
    public ResponseEntity<ApiResponse<IotDeviceResponse>> updateDevice(@PathVariable Long id,
                                                                       @RequestBody IotDeviceUpdateRequest req) {
        IotDeviceResponse data = iotIntegrationService.updateDevice(id, req);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "IoT device updated", data));
    }

    @GetMapping("/api/maintenance/iot/devices")
    public ResponseEntity<ApiResponse<IotDeviceListResponse>> listDevices(Pageable pageable,
                                                                          @RequestParam(required = false) Integer offlineAfterMinutes) {
        IotDeviceListResponse data = iotIntegrationService.listDevices(pageable, offlineAfterMinutes);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "IoT devices fetched", data));
    }

    @DeleteMapping("/api/maintenance/iot/devices/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDevice(@PathVariable Long id) {
        iotIntegrationService.deleteDevice(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "IoT device disabled", null));
    }

    // Public endpoint for physical devices (authorized via device credentials)
    @PostMapping("/api/iot/ingest")
    public ResponseEntity<ApiResponse<IotTelemetryIngestResponse>> ingest(@Valid @RequestBody IotTelemetryIngestRequest req) {
        IotTelemetryIngestResponse data = iotIntegrationService.ingestTelemetry(req);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Telemetry processed", data));
    }

    @GetMapping("/api/maintenance/iot/dashboard")
    public ResponseEntity<ApiResponse<IotDashboardResponse>> dashboard(
            @RequestParam(required = false) Long assetId,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) IotAlertSeverity severity,
            @RequestParam(required = false) Integer offlineAfterMinutes
    ) {
        IotDashboardResponse data = iotIntegrationService.getDashboard(assetId, location, severity, offlineAfterMinutes);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "IoT dashboard fetched", data));
    }

    @PostMapping("/api/maintenance/iot/alerts/{id}/acknowledge")
    public ResponseEntity<ApiResponse<IotAlertResponse>> acknowledgeAlert(@PathVariable Long id,
                                                                          @RequestBody(required = false) IotAlertActionRequest req) {
        IotAlertResponse data = iotIntegrationService.acknowledgeAlert(id, req);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Alert acknowledged", data));
    }

    @PostMapping("/api/maintenance/iot/alerts/{id}/resolve")
    public ResponseEntity<ApiResponse<IotAlertResponse>> resolveAlert(@PathVariable Long id,
                                                                      @RequestBody(required = false) IotAlertActionRequest req) {
        IotAlertResponse data = iotIntegrationService.resolveAlert(id, req);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Alert resolved", data));
    }

    @PostMapping("/api/maintenance/iot/sample-data")
    public ResponseEntity<ApiResponse<List<IotTelemetryIngestResponse>>> sampleData(@Valid @RequestBody IotSampleDataRequest req) {
        List<IotTelemetryIngestResponse> data = iotIntegrationService.generateSampleData(req);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Sample telemetry generated", data));
    }
}
