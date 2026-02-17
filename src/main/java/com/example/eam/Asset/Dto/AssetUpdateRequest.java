// AssetBasicPatchRequest.java
package com.example.eam.Asset.Dto;

import com.example.eam.Enum.AssetCriticality;
import com.example.eam.Enum.AssetStatus;

import lombok.Data;

@Data
public class AssetUpdateRequest {
    private String assetName;
    private String shortDescription;
    private String assetCategory;
    private String assetType;
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
    private java.time.LocalDate manufactureDate;
}
