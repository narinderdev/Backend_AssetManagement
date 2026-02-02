package com.example.eam.Procurement.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePurchaseOrderRequest {

    @NotNull
    private Long vendorId;

    @NotBlank
    private String createdByUserId;

    private Long mrId;

    private LocalDate requiredDeliveryDate;

    private String remarks;

    private String department;
    private String glAccountString;

    private String shipToType; // WAREHOUSE or WORK_SITE
    private Long shipToWarehouseId;
    private Long shipToWorkOrderId;

    @NotEmpty
    @Valid
    private List<PurchaseOrderLineRequest> lines;
}
