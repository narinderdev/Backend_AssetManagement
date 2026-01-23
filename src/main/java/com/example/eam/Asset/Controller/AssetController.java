package com.example.eam.Asset.Controller;

import com.example.eam.Asset.Dto.*;
import com.example.eam.Asset.Service.AssetService;
import com.example.eam.Common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    // 1. CREATE BASIC ASSET
    @PostMapping
    public ResponseEntity<ApiResponse<AssetDetailsResponse>> createAsset(
            @Validated @RequestBody CreateAssetDto request) {

        AssetDetailsResponse response = assetService.createAsset(request);
        ApiResponse<AssetDetailsResponse> apiResponse = ApiResponse.successResponse(HttpStatus.CREATED.value(),
                "Asset created successfully", response);
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    // 2. POST per section (Location & Org)
    @PostMapping("/{id}/location")
    public ResponseEntity<ApiResponse<AssetDetailsResponse>> saveLocationOrg(
            @PathVariable Long id,
            @Validated @RequestBody AssetLocationDto request) {

        AssetDetailsResponse response = assetService.saveLocationOrg(id, request);
        ApiResponse<AssetDetailsResponse> apiResponse = ApiResponse.successResponse(HttpStatus.OK.value(),
                "Asset location and organization details saved successfully", response);
        return ResponseEntity.ok(apiResponse);
    }

    // Technical
    @PostMapping("/{id}/technical")
    public ResponseEntity<ApiResponse<AssetDetailsResponse>> saveTechnicalDetails(
            @PathVariable Long id,
            @RequestBody AssetTechnicalDetailsDto request) {

        AssetDetailsResponse response = assetService.saveTechnicalDetails(id, request);
        ApiResponse<AssetDetailsResponse> apiResponse = ApiResponse.successResponse(HttpStatus.OK.value(),
                "Asset technical details saved successfully", response);
        return ResponseEntity.ok(apiResponse);
    }

    // Financial
    @PostMapping("/{id}/financial")
    public ResponseEntity<ApiResponse<AssetDetailsResponse>> saveFinancialDetails(
            @PathVariable Long id,
            @RequestBody AssetFinancialDetailsDto request) {

        AssetDetailsResponse response = assetService.saveFinancialDetails(id, request);
        ApiResponse<AssetDetailsResponse> apiResponse = ApiResponse.successResponse(HttpStatus.OK.value(),
                "Asset financial details saved successfully", response);
        return ResponseEntity.ok(apiResponse);
    }

    // Warranty & Lifecycle
    @PostMapping("/{id}/warranty")
    public ResponseEntity<ApiResponse<AssetDetailsResponse>> saveWarrantyLifecycle(
            @PathVariable Long id,
            @RequestBody AssetWarrantyLifecycleDto request) {

        AssetDetailsResponse response = assetService.saveWarrantyLifecycle(id, request);
        ApiResponse<AssetDetailsResponse> apiResponse = ApiResponse.successResponse(HttpStatus.OK.value(),
                "Asset warranty and lifecycle details saved successfully", response);
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/{id}/insurance")
    public ResponseEntity<ApiResponse<AssetDetailsResponse>> saveInsurance(
            @PathVariable Long id,
            @Validated @RequestBody AssetInsuranceDto request) {

        AssetDetailsResponse response = assetService.saveInsurance(id, request);
        ApiResponse<AssetDetailsResponse> apiResponse = ApiResponse.successResponse(HttpStatus.OK.value(),
                "Asset insurance saved successfully", response);
        return ResponseEntity.ok(apiResponse);
    }

    // Safety & Operations
    @PostMapping("/{id}/safety")
    public ResponseEntity<ApiResponse<AssetDetailsResponse>> saveSafetyOperations(
            @PathVariable Long id,
            @RequestBody AssetSafetyOperationsDto request) {

        AssetDetailsResponse response = assetService.saveSafetyOperations(id, request);
        ApiResponse<AssetDetailsResponse> apiResponse = ApiResponse.successResponse(HttpStatus.OK.value(),
                "Asset safety operations saved successfully", response);
        return ResponseEntity.ok(apiResponse);
    }

    // 3. SINGLE PATCH UPDATE (any section)
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AssetDetailsResponse>> patchAsset(
            @PathVariable Long id,
            @RequestBody AssetPatchRequest request) {

        AssetDetailsResponse response = assetService.patchAsset(id, request);
        ApiResponse<AssetDetailsResponse> apiResponse = ApiResponse.successResponse(HttpStatus.OK.value(),
                "Asset updated successfully", response);
        return ResponseEntity.ok(apiResponse);
    }

    // 4. DELETE ASSET
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAsset(@PathVariable Long id) {
        assetService.deleteAsset(id);
        ApiResponse<Void> apiResponse = ApiResponse.successResponse(HttpStatus.NO_CONTENT.value(),
                "Asset deleted successfully", null);
        return ResponseEntity.noContent().build(); 
    }

    // 5. GET SINGLE ASSET
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AssetDetailsResponse>> getAsset(@PathVariable Long id) {
        AssetDetailsResponse response = assetService.getAssetDetails(id);
        ApiResponse<AssetDetailsResponse> apiResponse = ApiResponse.successResponse(HttpStatus.OK.value(),
                "Asset details fetched successfully", response);
        return ResponseEntity.ok(apiResponse);
    }

    // 6. LIST ALL ASSETS (paginated)
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AssetDetailsResponse>>> listAssets(Pageable pageable) {
        Page<AssetDetailsResponse> page = assetService.listAssets(pageable);
        ApiResponse<Page<AssetDetailsResponse>> apiResponse = ApiResponse.successResponse(HttpStatus.OK.value(),
                "Assets list fetched successfully", page);
        return ResponseEntity.ok(apiResponse);
    }
}
