package com.example.eam.Asset.Dto;


import com.example.eam.Enum.AssetCriticality;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssetTypeUpdateRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    private Long assetCategoryId;
    private AssetCriticality defaultCriticality;
    private String defaultGlAccount;
    private String utilityAccount;
    private String retirementAccount;
    private Boolean insuranceRequired;
    private Boolean active;
}
