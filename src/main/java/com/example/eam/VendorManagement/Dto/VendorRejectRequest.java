package com.example.eam.VendorManagement.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VendorRejectRequest {
    @NotBlank
    private String comment;
}
