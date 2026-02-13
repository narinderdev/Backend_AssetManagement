package com.example.eam.InventoryManagement.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Common.PageResponse;
import com.example.eam.InventoryManagement.Dto.*;
import com.example.eam.InventoryManagement.Service.InventoryItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory-items")
@RequiredArgsConstructor
public class InventoryItemController {

    private final InventoryItemService service;

    @PostMapping
    public ResponseEntity<ApiResponse<InventoryItemResponse>> create(@Valid @RequestBody InventoryItemCreateRequest request) {
        InventoryItemResponse data = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successResponse(HttpStatus.CREATED.value(), "Inventory item created successfully", data));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> patch(@PathVariable Long id,
                                                                    @RequestBody InventoryItemPatchRequest request) {
        InventoryItemResponse data = service.patch(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Inventory item updated successfully", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> get(@PathVariable Long id) {
        InventoryItemResponse data = service.get(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Inventory item fetched successfully", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<InventoryItemResponse>>> list(Pageable pageable) {
        Page<InventoryItemResponse> data = service.list(pageable);
        return ResponseEntity.ok(
                ApiResponse.successResponse(
                        HttpStatus.OK.value(),
                        "Inventory items fetched successfully",
                        PageResponse.from(data)
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Inventory item deleted successfully", null));
    }

    // Reorder action
    @PostMapping("/{id}/reorder")
    public ResponseEntity<ApiResponse<InventoryReorderResponse>> reorder(@PathVariable Long id,
                                                                         @Valid @RequestBody InventoryReorderCreateRequest request) {
        InventoryReorderResponse data = service.reorder(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Reorder request created successfully", data));
    }
}

