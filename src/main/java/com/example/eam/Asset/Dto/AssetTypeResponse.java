package com.example.eam.Asset.Dto;


import com.example.eam.Enum.AssetCriticality;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssetTypeResponse {
    private Long id;
    private String code;
    private String name;
    private Long companyId;
    private Long assetCategoryId;
    private String assetCategory;
    private AssetCriticality defaultCriticality;
    private String defaultGlAccount;
    private String utilityAccount;
    private String retirementAccount;
    private Boolean insuranceRequired;
    private Boolean active;
}
