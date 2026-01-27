package com.example.eam.VendorManagement.Dto;


import com.example.eam.Enum.PaymentTerms;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class VendorCreateRequest {

    @NotBlank
    private String vendorName;

    private String vendorId;

    @NotBlank
    private String taxId;

    private String address;

    @NotBlank
    private String contactPerson;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String phone;

    @NotNull
    private PaymentTerms paymentTerms;

    @Min(1)
    @Max(5)
    private Integer rating;

    // Optional: if not sent => default true
    private Boolean active;
}

