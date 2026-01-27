package com.example.eam.VendorManagement.Dto;


import com.example.eam.Enum.PaymentTerms;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class VendorPatchRequest {

    private String vendorName;
    private String address;
    private String contactPerson;

    @Email
    private String email;

    private String phone;
    private String taxId;
    private PaymentTerms paymentTerms;

    @Min(1)
    @Max(5)
    private Integer rating;

    private Boolean active;
}

