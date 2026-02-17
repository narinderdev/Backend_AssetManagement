package com.example.eam.InventoryManagement.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Enum.InventoryReferenceType;
import com.example.eam.Enum.InventoryTransactionType;
import com.example.eam.InventoryManagement.Dto.InventoryReportResponse;
import com.example.eam.InventoryManagement.Dto.InventoryReportView;
import com.example.eam.InventoryManagement.Service.InventoryReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory/report")
@RequiredArgsConstructor
public class InventoryReportController {

    private final InventoryReportService reportService;

    @GetMapping
    public ResponseEntity<ApiResponse<InventoryReportResponse>> getReport(
            @RequestParam(value = "view", defaultValue = "ITEMS") InventoryReportView view,
            @RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam(value = "lowStockOnly", defaultValue = "false") boolean lowStockOnly,
            @RequestParam(value = "transactionType", required = false) InventoryTransactionType txnType,
            @RequestParam(value = "referenceType", required = false) InventoryReferenceType refType,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "topN", defaultValue = "5") int topN,
            Pageable pageable
    ) {
        InventoryReportResponse data = reportService.getReport(
                view,
                warehouseId,
                lowStockOnly,
                txnType,
                refType,
                period,
                pageable,
                topN
        );
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Inventory report fetched", data));
    }
}
