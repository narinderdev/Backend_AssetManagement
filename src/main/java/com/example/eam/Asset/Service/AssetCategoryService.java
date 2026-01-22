package com.example.eam.Asset.Service;

import com.example.eam.Asset.Dto.AssetCategoryResponse;
import com.example.eam.Asset.Entity.AssetCategory;
import com.example.eam.Asset.Repository.AssetCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetCategoryService {

    private final AssetCategoryRepository assetCategoryRepository;

    @Transactional(readOnly = true)
    public List<AssetCategoryResponse> listCategories() {
        return assetCategoryRepository.findAllByOrderByNameAsc().stream()
                .map(category -> AssetCategoryResponse.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .build())
                .toList();
    }

    @Transactional
    public AssetCategory getOrCreateByName(String rawName) {
        String normalized = normalizeName(rawName);
        return assetCategoryRepository.findByNameIgnoreCase(normalized)
                .orElseGet(() -> {
                    try {
                        return assetCategoryRepository.save(
                                AssetCategory.builder().name(normalized).build()
                        );
                    } catch (DataIntegrityViolationException ex) {
                        return assetCategoryRepository.findByNameIgnoreCase(normalized)
                                .orElseThrow(() -> ex);
                    }
                });
    }

    private String normalizeName(String rawName) {
        if (rawName == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset category is required");
        }
        String trimmed = rawName.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset category cannot be blank");
        }
        return trimmed;
    }
}
