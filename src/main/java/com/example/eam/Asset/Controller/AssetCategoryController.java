package com.example.eam.Asset.Controller;

import com.example.eam.Asset.Dto.AssetCategoryResponse;
import com.example.eam.Asset.Service.AssetCategoryService;
import com.example.eam.Common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/asset-categories")
@RequiredArgsConstructor
public class AssetCategoryController {

    private final AssetCategoryService assetCategoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AssetCategoryResponse>>> listCategories() {
        List<AssetCategoryResponse> categories = assetCategoryService.listCategories();
        return ResponseEntity.ok(
                ApiResponse.successResponse(HttpStatus.OK.value(),
                        "Asset categories fetched successfully",
                        categories)
        );
    }
}
