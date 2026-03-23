package com.example.eam.CompanyManagement.Dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CompanyResponse {
    private Long id;
    private String companyLegalName;
    private String companyTradeName;
    private String companyNumber;
    private String address;
    private String city;
    private String country;
    private String postalCode;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
