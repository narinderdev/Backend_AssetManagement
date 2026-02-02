package com.example.eam.Asset.Controller;


import com.example.eam.Asset.Dto.AssetTypeResponse;
import com.example.eam.Asset.Dto.AssetTypeCreateRequest;
import com.example.eam.Asset.Dto.AssetTypeUpdateRequest;
import com.example.eam.Asset.Service.AssetTypeService;
import com.example.eam.Common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asset-types")
@RequiredArgsConstructor
public class AssetTypeController {

    private final AssetTypeService assetTypeService;

    @PostMapping
    public ResponseEntity<ApiResponse<AssetTypeResponse>> createAssetType(
            @Validated @RequestBody AssetTypeCreateRequest request) {

        AssetTypeResponse response = assetTypeService.create(request);
        ApiResponse<AssetTypeResponse> apiResponse = ApiResponse.successResponse(
                HttpStatus.CREATED.value(),
                "Asset type created successfully",
                response
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AssetTypeResponse>>> listAssetTypes() {
        List<AssetTypeResponse> response = assetTypeService.listAll();
        ApiResponse<List<AssetTypeResponse>> apiResponse = ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Asset types fetched successfully",
                response
        );
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AssetTypeResponse>> getAssetType(@PathVariable Long id) {
        AssetTypeResponse response = assetTypeService.getById(id);
        ApiResponse<AssetTypeResponse> apiResponse = ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Asset type fetched successfully",
                response
        );
        return ResponseEntity.ok(apiResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AssetTypeResponse>> updateAssetType(
            @PathVariable Long id,
            @Validated @RequestBody AssetTypeUpdateRequest request
    ) {
        AssetTypeResponse response = assetTypeService.update(id, request);
        ApiResponse<AssetTypeResponse> apiResponse = ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Asset type updated successfully",
                response
        );
        return ResponseEntity.ok(apiResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAssetType(@PathVariable Long id) {
        assetTypeService.delete(id);
        ApiResponse<Void> apiResponse = ApiResponse.successResponse(
                HttpStatus.NO_CONTENT.value(),
                "Asset type deleted successfully",
                null
        );
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(apiResponse);
    }
}
