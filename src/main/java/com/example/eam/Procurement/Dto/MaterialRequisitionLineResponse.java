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
public class MaterialRequisitionLineResponse {

    private Long id;
    private Long itemId;
    private BigDecimal requestedQty;
    private BigDecimal costPerUnit;
    private String uom;
    private String remarks;
}
