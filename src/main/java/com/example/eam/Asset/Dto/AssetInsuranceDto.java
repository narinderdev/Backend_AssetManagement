package com.example.eam.Asset.Dto;


import com.example.eam.Enum.InsuranceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AssetInsuranceDto {

    @NotBlank
    private String insuranceProvider;

    @NotBlank
    private String policyNumber;

    @NotNull
    private LocalDate policyStartDate;

    @NotNull
    private LocalDate policyExpiryDate;

    private InsuranceStatus insuranceStatus;
    private String policyType;
    private String certificateUrl;
    private BigDecimal coverageAmount;
    private BigDecimal premiumAmount;
}
