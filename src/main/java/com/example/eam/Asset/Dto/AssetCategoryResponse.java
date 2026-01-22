package com.example.eam.Asset.Dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssetCategoryResponse {

    private Long id;
    private String name;
}
