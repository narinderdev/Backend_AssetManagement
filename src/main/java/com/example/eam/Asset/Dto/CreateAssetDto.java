package com.example.eam.Asset.Dto;


import com.example.eam.Enum.AssetCriticality;
import com.example.eam.Enum.AssetStatus;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAssetDto {

    @Size(max = 64)
    private String assetId;          // Asset ID (optional - auto-generated if missing)

    @NotBlank
    private String assetName;        // Asset Name*

    private String shortDescription;

    @NotBlank
    private String assetCategory;    // Asset Category*

    private String assetType;
    private Long assetTypeId;

    @NotNull
    private AssetStatus status;      // Status*

    private AssetCriticality criticality;
    private String ownership;
    private String assetTag;         // Tag / Barcode / RFID

    @Size(max = 128)
    private String functionalClass;

    @Size(max = 128)
    private String retirementUnit;

    @Size(max = 128)
    private String utilityAccount;

    @Size(max = 128)
    private String propertyUnit;

    @Size(max = 128)
    private String propertyGroup;

    @Size(max = 128)
    private String serialNumber;

    @Size(max = 128)
    private String modelNumber;

    private LocalDate manufactureDate;
}

