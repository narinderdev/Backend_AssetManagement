package com.example.eam.Asset.Dto;

import lombok.Data;

@Data
public class AssetPatchRequest {

    private AssetBasicPatchRequest basic;
    private AssetLocationDto locationOrg;
    private AssetTechnicalDetailsDto technicalDetails;
    private AssetFinancialDetailsDto financialDetails;
    private AssetWarrantyLifecycleDto warrantyLifecycle;
    private AssetSafetyOperationsDto safetyOperations;
    private AssetInsuranceDto insurance;
}

