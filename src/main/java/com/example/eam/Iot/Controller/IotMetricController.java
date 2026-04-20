package com.example.eam.Iot.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Common.PageResponse;
import com.example.eam.Iot.Dto.IotMetricCreateRequest;
import com.example.eam.Iot.Dto.IotMetricPatchRequest;
import com.example.eam.Iot.Dto.IotMetricResponse;
import com.example.eam.Iot.Service.IotMetricCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/iot/metrics")
@RequiredArgsConstructor
public class IotMetricController {

    private final IotMetricCatalogService metricCatalogService;

    @PostMapping
    public ResponseEntity<ApiResponse<IotMetricResponse>> create(@Valid @RequestBody IotMetricCreateRequest request) {
        IotMetricResponse response = metricCatalogService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successResponse(HttpStatus.CREATED.value(), "IoT metric created", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IotMetricResponse>> get(@PathVariable Long id) {
        IotMetricResponse response = metricCatalogService.get(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "IoT metric fetched", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<IotMetricResponse>>> list(Pageable pageable) {
        Page<IotMetricResponse> page = metricCatalogService.list(pageable);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "IoT metrics fetched", PageResponse.from(page)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<IotMetricResponse>> patch(@PathVariable Long id,
                                                                @RequestBody IotMetricPatchRequest request) {
        IotMetricResponse response = metricCatalogService.patch(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "IoT metric updated", response));
    }
}

