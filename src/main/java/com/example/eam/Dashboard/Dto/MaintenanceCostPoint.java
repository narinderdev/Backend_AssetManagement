package com.example.eam.Dashboard.Dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MaintenanceCostPoint {
    private String month;
    private BigDecimal cost;
    private String currency;
    private String unit;
}
