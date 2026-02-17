package com.example.eam.InventoryManagement.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.InventoryManagement.Dto.InventoryAuditLogResponse;
import com.example.eam.InventoryManagement.Service.InventoryAuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory/audit-logs")
@RequiredArgsConstructor
public class InventoryAuditLogController {

    private final InventoryAuditLogService service;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryAuditLogResponse>> get(@PathVariable Long id) {
        InventoryAuditLogResponse data = service.get(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Audit log fetched", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<InventoryAuditLogResponse>>> list(
            Pageable pageable) {
        Page<InventoryAuditLogResponse> data = service.list(pageable);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Audit logs fetched", data));
    }
}
