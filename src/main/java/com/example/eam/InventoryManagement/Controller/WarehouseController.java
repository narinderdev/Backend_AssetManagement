package com.example.eam.InventoryManagement.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.InventoryManagement.Dto.WarehouseCreateRequest;
import com.example.eam.InventoryManagement.Dto.WarehousePatchRequest;
import com.example.eam.InventoryManagement.Dto.WarehouseResponse;
import com.example.eam.InventoryManagement.Service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PostMapping
    public ResponseEntity<ApiResponse<WarehouseResponse>> create(@Valid @RequestBody WarehouseCreateRequest request) {
        WarehouseResponse data = warehouseService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successResponse(HttpStatus.CREATED.value(), "Warehouse created successfully", data));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<WarehouseResponse>> patch(@PathVariable Long id,
                                                                @RequestBody WarehousePatchRequest request) {
        WarehouseResponse data = warehouseService.patch(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Warehouse updated successfully", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WarehouseResponse>> get(@PathVariable Long id) {
        WarehouseResponse data = warehouseService.get(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Warehouse fetched successfully", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WarehouseResponse>>> listActive() {
        List<WarehouseResponse> data = warehouseService.listActive();
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Warehouses fetched successfully", data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        warehouseService.delete(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Warehouse deleted successfully", null));
    }
}
