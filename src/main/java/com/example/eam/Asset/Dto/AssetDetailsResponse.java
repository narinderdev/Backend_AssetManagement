package com.example.eam.Asset.Dto;


import com.example.eam.Enum.AssetCriticality;
import com.example.eam.Enum.AssetStatus;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonPropertyOrder({
    "id",
    "companyId",
    "assetId",
    "assetName",
    "assetTag",
    "assetType",
    "assetTypeId",
    "assetTypeCode",
    "assetCategory",
    "status",
    "criticality",
    "shortDescription",
    "ownership",
    "parentAssetId",
    "functionalClass",
    "retirementUnit",
    "utilityAccount",
    "propertyUnit",
    "propertyGroup",
    "serialNumber",
    "modelNumber",
    "manufactureDate",
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
    private Long companyId;
    private String assetId;
    private String assetName;
    private String shortDescription;
    private String assetCategory;
    private String assetType;
    private Long assetTypeId;
    private String assetTypeCode;
    private Long parentAssetId;
    private AssetStatus status;
    private AssetCriticality criticality;
    private String ownership;
    private String assetTag;
    private String functionalClass;
    private String retirementUnit;
    private String utilityAccount;
    private String propertyUnit;
    private String propertyGroup;
    private String serialNumber;
    private String modelNumber;
    private LocalDate manufactureDate;

    private AssetLocationDto location;
    private java.util.List<com.example.eam.Maintenance.Predictive.Dto.AssetThresholdResponse> predictiveThresholds;
    private AssetTechnicalDetailsDto technicalDetails;
    private AssetFinancialDetailsDto financialDetails;
    private AssetWarrantyLifecycleDto warrantyLifecycle;
    private AssetInsuranceDto insurance;
    private AssetSafetyOperationsDto safetyOperations;
}

