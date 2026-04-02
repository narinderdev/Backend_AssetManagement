package com.example.eam.Iot.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Common.PageResponse;
import com.example.eam.Enum.IotAlertSeverity;
import com.example.eam.Enum.IotAlertStatus;
import com.example.eam.Iot.Dto.IotAlertActionRequest;
import com.example.eam.Iot.Dto.IotAlertResponse;
import com.example.eam.Iot.Service.IotAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/iot/alerts")
@RequiredArgsConstructor
public class IotAlertController {

    private final IotAlertService alertService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<IotAlertResponse>>> list(
            @RequestParam(required = false) Long assetId,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) IotAlertSeverity severity,
            @RequestParam(required = false) IotAlertStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable) {
        Page<IotAlertResponse> page = alertService.list(assetId, location, severity, status, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "IoT alerts fetched", PageResponse.from(page)));
    }

    @PostMapping("/{id}/ack")
    public ResponseEntity<ApiResponse<IotAlertResponse>> acknowledge(@PathVariable Long id,
                                                                     @RequestBody(required = false) IotAlertActionRequest request) {
        IotAlertResponse response = alertService.acknowledge(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "IoT alert acknowledged", response));
    }

    @PostMapping("/{id}/suppress")
    public ResponseEntity<ApiResponse<IotAlertResponse>> suppress(@PathVariable Long id,
                                                                  @RequestBody(required = false) IotAlertActionRequest request) {
        IotAlertResponse response = alertService.suppress(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "IoT alert suppressed", response));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<IotAlertResponse>> resolve(@PathVariable Long id,
                                                                 @RequestBody(required = false) IotAlertActionRequest request) {
        IotAlertResponse response = alertService.resolve(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "IoT alert resolved", response));
    }
}
