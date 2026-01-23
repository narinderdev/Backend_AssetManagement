package com.example.eam.Asset.Dto;


import com.example.eam.Enum.AssetCriticality;
import com.example.eam.Enum.AssetStatus;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonPropertyOrder({
    "id",
    "assetId",
    "assetName",
    "assetTag",
    "assetType",
    "assetCategory",
    "status",
    "criticality",
    "shortDescription",
    "ownership",
    "parentAssetId",
    "location",
    "predictiveThresholds",
    "technicalDetails",
    "financialDetails",
    "warrantyLifecycle",
    "insurance",
    "safetyOperations"
})
public class AssetDetailsResponse {

    private Long id;
    private String assetId;
    private String assetName;
    private String shortDescription;
    private String assetCategory;
    private String assetType;
    private Long parentAssetId;
    private AssetStatus status;
    private AssetCriticality criticality;
    private String ownership;
    private String assetTag;

    private AssetLocationDto location;
    private java.util.List<com.example.eam.Maintenance.Predictive.Dto.AssetThresholdResponse> predictiveThresholds;
    private AssetTechnicalDetailsDto technicalDetails;
    private AssetFinancialDetailsDto financialDetails;
    private AssetWarrantyLifecycleDto warrantyLifecycle;
    private AssetInsuranceDto insurance;
    private AssetSafetyOperationsDto safetyOperations;
}

