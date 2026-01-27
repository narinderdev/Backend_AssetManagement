package com.example.eam.VendorManagement.Dto;


import com.example.eam.Enum.PaymentTerms;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VendorResponse {
    private Long id;
    private String vendorId;

    private String vendorName;
    private String taxId;
    private String address;
    private String contactPerson;
    private String email;
    private String phone;

    private PaymentTerms paymentTerms;
    private Integer rating;
    private boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

