package com.example.eam.InventoryManagement.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InventoryReconciliationDecisionRequest {
    @NotBlank
    private String actor; // approvedBy or rejectedBy
    private String comment;
}
