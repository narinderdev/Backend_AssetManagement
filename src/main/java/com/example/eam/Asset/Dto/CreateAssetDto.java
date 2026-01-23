package com.example.eam.Asset.Dto;


import com.example.eam.Enum.AssetCriticality;
import com.example.eam.Enum.AssetStatus;

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
}

