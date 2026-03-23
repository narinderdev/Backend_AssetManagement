package com.example.eam.CompanyManagement.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompanyCreateRequest {

    @NotBlank
    private String companyLegalName;

    @NotBlank
    private String companyTradeName;

    @NotBlank
    private String companyNumber;

    @NotBlank
    private String address;

    @NotBlank
    private String city;

    @NotBlank
    private String country;

    @NotBlank
    private String postalCode;

    private Boolean active;
}
