package com.example.eam.Procurement.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Common.PageResponse;
import com.example.eam.Procurement.Dto.*;
import com.example.eam.Procurement.Service.MaterialRequisitionService;
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
@RequestMapping("/api/procurement/mr")
@RequiredArgsConstructor
@Validated
public class MaterialRequisitionController {

    private final MaterialRequisitionService materialRequisitionService;
    private final PurchaseOrderService purchaseOrderService;

    @PostMapping
    public ResponseEntity<ApiResponse<MaterialRequisitionResponse>> create(
            @Valid @RequestBody CreateMaterialRequisitionRequest request) {
        MaterialRequisitionResponse data = materialRequisitionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successResponse(HttpStatus.CREATED.value(), "Material Requisition created", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MaterialRequisitionResponse>>> list(Pageable pageable) {
        Page<MaterialRequisitionResponse> data = materialRequisitionService.list(pageable);
        return ResponseEntity.ok(
                ApiResponse.successResponse(
                        HttpStatus.OK.value(),
                        "Material Requisitions fetched",
                        PageResponse.from(data)
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MaterialRequisitionResponse>> get(@PathVariable Long id) {
        MaterialRequisitionResponse data = materialRequisitionService.get(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Material Requisition fetched", data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MaterialRequisitionResponse>> update(@PathVariable Long id,
                                                                           @Valid @RequestBody UpdateMaterialRequisitionRequest request) {
        MaterialRequisitionResponse data = materialRequisitionService.update(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Material Requisition updated", data));
    }

    // @PostMapping("/{id}/submit")
    // public ResponseEntity<ApiResponse<MaterialRequisitionResponse>> submit(@PathVariable Long id) {
    //     MaterialRequisitionResponse data = materialRequisitionService.submit(id);
    //     return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Material Requisition submitted", data));
    // }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<MaterialRequisitionResponse>> approve(@PathVariable Long id,
                                                                            @Valid @RequestBody MrApprovalRequest request) {
        MaterialRequisitionResponse data = materialRequisitionService.approve(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Material Requisition approved", data));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<MaterialRequisitionResponse>> reject(@PathVariable Long id,
                                                                           @Valid @RequestBody MrRejectionRequest request) {
        MaterialRequisitionResponse data = materialRequisitionService.reject(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Material Requisition rejected", data));
    }

    // @PostMapping("/{id}/cancel")
    // public ResponseEntity<ApiResponse<MaterialRequisitionResponse>> cancel(@PathVariable Long id) {
    //     MaterialRequisitionResponse data = materialRequisitionService.cancel(id);
    //     return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Material Requisition cancelled", data));
    // }

    @PostMapping("/{id}/convert-to-po")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> convertToPo(@PathVariable Long id,
                                                                          @Valid @RequestBody ConvertMrToPoRequest request) {
        PurchaseOrderResponse data = purchaseOrderService.convertMrToPo(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successResponse(HttpStatus.CREATED.value(), "Purchase Order created from MR", data));
    }
}
