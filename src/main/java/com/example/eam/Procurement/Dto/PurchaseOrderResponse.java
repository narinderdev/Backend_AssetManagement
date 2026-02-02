package com.example.eam.Procurement.Dto;

import com.example.eam.Procurement.Enum.PurchaseOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderResponse {

    private Long id;
    private String poNumber;
    private Long vendorId;
    private String vendorCode;
    private String vendorName;
    private String vendorEmail;
    private String vendorPhone;
    private String vendorContactPerson;
    private String glAccountString;
    private Long mrId;
    private String department;
    private String shipToType;
    private Long shipToWarehouseId;
    private Long shipToWorkOrderId;
    private PurchaseOrderStatus status;
    private LocalDate requiredDeliveryDate;
    private String remarks;
    private String createdByUserId;
    private Instant createdAt;
    private Instant updatedAt;
    private List<PurchaseOrderLineResponse> lines;
}
