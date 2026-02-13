package com.example.eam.Reports.Controller;

import com.example.eam.Asset.Dto.AssetDetailsResponse;
import com.example.eam.Asset.Service.AssetService;
import com.example.eam.Common.ApiResponse;
import com.example.eam.Common.PageResponse;
import com.example.eam.Enum.AssetCriticality;
import com.example.eam.Enum.AssetStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.Set;

@RestController
@RequestMapping("/api/reports/assets")
@RequiredArgsConstructor
public class AssetReportController {

    private final AssetService assetService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AssetDetailsResponse>>> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "warrantyExpiryDays", required = false) Integer warrantyExpiryDays,
            @RequestParam(value = "criticality", required = false) AssetCriticality criticality,
            @RequestParam(value = "assetTypeId", required = false) Long assetTypeId,
            Pageable pageable
    ) {
        Set<AssetStatus> statuses = resolveStatusFilter(status);
        Page<AssetDetailsResponse> data = assetService.reportAssets(
                statuses, criticality, assetTypeId, warrantyExpiryDays, pageable
        );
        return ResponseEntity.ok(
                ApiResponse.successResponse(HttpStatus.OK.value(), "Asset report fetched", PageResponse.from(data))
        );
    }

    private Set<AssetStatus> resolveStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return EnumSet.allOf(AssetStatus.class);
        }
        String normalized = status.trim().toUpperCase();
        try {
            return EnumSet.of(AssetStatus.valueOf(normalized));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "status must be a valid AssetStatus"
            );
        }
    }
}
