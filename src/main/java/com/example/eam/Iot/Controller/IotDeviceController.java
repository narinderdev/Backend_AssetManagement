package com.example.eam.Iot.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Common.PageResponse;
import com.example.eam.Iot.Dto.*;
import com.example.eam.Iot.Service.IotDeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/iot/devices")
@RequiredArgsConstructor
public class IotDeviceController {

    private final IotDeviceService deviceService;

    @PostMapping
    public ResponseEntity<ApiResponse<IotDeviceCreateResponse>> create(@Valid @RequestBody IotDeviceCreateRequest request) {
        IotDeviceCreateResponse response = deviceService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successResponse(HttpStatus.CREATED.value(), "IoT device created", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IotDeviceResponse>> get(@PathVariable Long id) {
        IotDeviceResponse response = deviceService.get(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "IoT device fetched", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<IotDeviceResponse>>> list(Pageable pageable) {
        Page<IotDeviceResponse> page = deviceService.list(pageable);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "IoT devices fetched", PageResponse.from(page)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<IotDeviceResponse>> patch(@PathVariable Long id,
                                                                @RequestBody IotDevicePatchRequest request) {
        IotDeviceResponse response = deviceService.patch(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "IoT device updated", response));
    }

    @PostMapping("/{id}/rotate-secret")
    public ResponseEntity<ApiResponse<IotRotateSecretResponse>> rotateSecret(@PathVariable Long id) {
        IotRotateSecretResponse response = deviceService.rotateSecret(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "IoT device secret rotated", response));
    }
}

