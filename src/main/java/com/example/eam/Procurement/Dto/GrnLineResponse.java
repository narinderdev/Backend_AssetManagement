package com.example.eam.Procurement.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrnLineResponse {

    private Long id;
    private Long poLineId;
    private Long itemId;
    private String itemName;
    private BigDecimal orderedQty;
    private BigDecimal receivedQty;
    private BigDecimal returnQty;
}
