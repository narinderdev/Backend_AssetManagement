package com.example.eam.CompanyManagement.Dto;

import lombok.Data;

@Data
public class CompanyPatchRequest {

    private String companyLegalName;
    private String companyTradeName;
    private String companyNumber;
    private String address;
    private String city;
    private String country;
    private String postalCode;
    private Boolean active;
}
