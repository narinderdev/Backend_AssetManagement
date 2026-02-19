package com.example.eam.Procurement.Dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGrnLineRequest {

    private Long poLineId;

    private Long itemId;

    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal orderedQty; // optional when poLineId is present; required when no PO

    @NotNull
    @DecimalMin(value = "0", inclusive = false, message = "Received quantity must be at least 1")
    private BigDecimal receivedQty;

    // Quantity being returned to vendor (informational); stock is updated only by receivedQty
    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal returnQty;

    // Required when returnQty > 0
    private String returnReason;
}
