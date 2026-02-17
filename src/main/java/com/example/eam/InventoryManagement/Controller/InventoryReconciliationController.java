package com.example.eam.InventoryManagement.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.InventoryManagement.Dto.InventoryReconciliationCreateRequest;
import com.example.eam.InventoryManagement.Dto.InventoryReconciliationUpdateRequest;
import com.example.eam.InventoryManagement.Dto.InventoryReconciliationResponse;
import com.example.eam.InventoryManagement.Service.InventoryReconciliationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory/reconciliations")
@RequiredArgsConstructor
public class InventoryReconciliationController {

    private final InventoryReconciliationService service;

    @PostMapping
    public ResponseEntity<ApiResponse<InventoryReconciliationResponse>> create(
            @Valid @RequestBody InventoryReconciliationCreateRequest req) {
        InventoryReconciliationResponse data = service.create(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successResponse(HttpStatus.CREATED.value(), "Reconciliation created", data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryReconciliationResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody InventoryReconciliationUpdateRequest req) {
        InventoryReconciliationResponse data = service.update(id, req);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Reconciliation updated", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryReconciliationResponse>> get(@PathVariable Long id) {
        InventoryReconciliationResponse data = service.get(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Reconciliation fetched", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<InventoryReconciliationResponse>>> list(Pageable pageable) {
        Page<InventoryReconciliationResponse> data = service.list(pageable);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Reconciliations fetched", data));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<InventoryReconciliationResponse>> approve(
            @PathVariable Long id,
            @Valid @RequestBody com.example.eam.InventoryManagement.Dto.InventoryReconciliationDecisionRequest req) {
        InventoryReconciliationResponse data = service.approve(id, req);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Reconciliation approved", data));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<InventoryReconciliationResponse>> reject(
            @PathVariable Long id,
            @Valid @RequestBody com.example.eam.InventoryManagement.Dto.InventoryReconciliationDecisionRequest req) {
        InventoryReconciliationResponse data = service.reject(id, req);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Reconciliation rejected", data));
    }

    @PatchMapping("/{id}/post")
    public ResponseEntity<ApiResponse<InventoryReconciliationResponse>> post(@PathVariable Long id) {
        InventoryReconciliationResponse data = service.post(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Reconciliation posted", data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Reconciliation deleted", null));
    }
}
