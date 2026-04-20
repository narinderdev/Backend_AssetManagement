package com.example.eam.Asset.Dto;


import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AssetLocationDto {

    @NotBlank
    private String location;     

    private String department;
    private String costCenter;
    private String assignedOwner;
    private String maintenanceTeam;

    @DecimalMin(value = "-90.0", message = "latitude must be greater than or equal to -90")
    @DecimalMax(value = "90.0", message = "latitude must be less than or equal to 90")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "longitude must be greater than or equal to -180")
    @DecimalMax(value = "180.0", message = "longitude must be less than or equal to 180")
    private BigDecimal longitude;
}
