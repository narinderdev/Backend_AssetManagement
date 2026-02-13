package com.example.eam.Procurement.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Common.PageResponse;
import com.example.eam.Procurement.Dto.CreatePurchaseOrderRequest;
import com.example.eam.Procurement.Dto.PurchaseOrderResponse;
import com.example.eam.Procurement.Dto.UpdatePurchaseOrderStatusRequest;
import com.example.eam.Procurement.Enum.PurchaseOrderStatus;
import com.example.eam.Procurement.Service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/procurement/po")
@RequiredArgsConstructor
@Validated
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> create(@Valid @RequestBody CreatePurchaseOrderRequest request) {
        PurchaseOrderResponse data = purchaseOrderService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successResponse(HttpStatus.CREATED.value(), "Purchase Order created", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PurchaseOrderResponse>>> list(
            Pageable pageable,
            @RequestParam(required = false) Long mrId
    ) {
        Page<PurchaseOrderResponse> data = purchaseOrderService.list(pageable, mrId);
        return ResponseEntity.ok(
                ApiResponse.successResponse(HttpStatus.OK.value(), "Purchase Orders fetched", PageResponse.from(data))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> get(@PathVariable Long id) {
        PurchaseOrderResponse data = purchaseOrderService.get(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Purchase Order fetched", data));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> updateStatus(@PathVariable Long id,
                                                                           @Valid @RequestBody UpdatePurchaseOrderStatusRequest request) {
        PurchaseOrderStatus newStatus = request.getNewStatus();
        PurchaseOrderResponse data = purchaseOrderService.updateStatus(id, newStatus, request.getRemarks());
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Purchase Order status updated", data));
    }
}
