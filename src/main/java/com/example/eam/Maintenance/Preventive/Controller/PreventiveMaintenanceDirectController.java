package com.example.eam.Maintenance.Preventive.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Maintenance.Preventive.Dto.PreventivePlanCreateRequest;
import com.example.eam.Maintenance.Preventive.Dto.PreventivePlanPatchRequest;
import com.example.eam.Maintenance.Preventive.Dto.PreventivePlanResponse;
import com.example.eam.Maintenance.Preventive.Service.PreventivePlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/maintenance/preventive")
@RequiredArgsConstructor
public class PreventiveMaintenanceDirectController {

    private final PreventivePlanService planService;

    @PostMapping
    public ResponseEntity<ApiResponse<PreventivePlanResponse>> create(@Valid @RequestBody PreventivePlanCreateRequest req) {
        PreventivePlanResponse data = planService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successResponse(HttpStatus.CREATED.value(), "Preventive plan created", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PreventivePlanResponse>> get(@PathVariable Long id) {
        PreventivePlanResponse data = planService.get(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Preventive plan fetched", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PreventivePlanResponse>>> list(Pageable pageable) {
        Page<PreventivePlanResponse> data = planService.list(pageable);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Preventive plans fetched", data));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<PreventivePlanResponse>> patch(@PathVariable Long id,
                                                                     @RequestBody PreventivePlanPatchRequest req) {
        PreventivePlanResponse data = planService.patch(id, req);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Preventive plan updated", data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        planService.delete(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Preventive plan deleted", null));
    }
}
