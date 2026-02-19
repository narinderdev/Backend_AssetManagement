package com.example.eam.Procurement.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorReturnResponse {
    private Long id;
    private Long grnId;
    private String grnNumber;
    private Long grnLineId;
    private Long poId;
    private Long poLineId;
    private Long vendorId;

    private Long itemDbId;
    private String itemId;
    private String skuNumber;
    private String itemName;

    private BigDecimal orderedQty;
    private BigDecimal receivedQty;
    private BigDecimal returnQty;          // qty in this transaction
    private BigDecimal totalReturnedQty;   // cumulative returned for the GRN line

    private BigDecimal unitCost;
    private BigDecimal returnCost;

    private Integer stockBefore;
    private Integer stockAfter;

    private String reason;
    private String performedBy;
    private Instant createdAt;
}
