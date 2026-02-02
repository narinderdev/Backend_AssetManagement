package com.example.eam.Asset.Service;


import com.example.eam.Asset.Dto.AssetTypeCreateRequest;
import com.example.eam.Asset.Dto.AssetTypeResponse;
import com.example.eam.Asset.Dto.AssetTypeUpdateRequest;
import com.example.eam.Asset.Entity.AssetCategory;
import com.example.eam.Asset.Entity.AssetType;
import com.example.eam.Asset.Repository.AssetTypeRepository;
import com.example.eam.Asset.Repository.AssetRepository;
import com.example.eam.Asset.Service.AssetCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetTypeService {

    private final AssetTypeRepository assetTypeRepository;
    private final AssetCategoryService assetCategoryService;
    private final AssetRepository assetRepository;

    @Transactional(readOnly = true)
    public AssetType getActiveAssetTypeOrThrow(Long id) {
        AssetType assetType = assetTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset type not found"));

        if (Boolean.FALSE.equals(assetType.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset type is inactive");
        }

        return assetType;
    }

    @Transactional
    public AssetTypeResponse create(AssetTypeCreateRequest request) {
        String code = normalize(request.getCode(), "Asset type code");
        String name = normalize(request.getName(), "Asset type name");

        if (assetTypeRepository.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Asset type code already exists");
        }
        if (assetTypeRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Asset type name already exists");
        }

        AssetCategory category = null;
        if (request.getAssetCategoryId() != null) {
            category = assetCategoryService.getByIdOrThrow(request.getAssetCategoryId());
        }

        AssetType assetType = AssetType.builder()
                .code(code)
                .name(name)
                .assetCategory(category)
                .defaultCriticality(request.getDefaultCriticality())
                .defaultGlAccount(trimToNull(request.getDefaultGlAccount()))
                .insuranceRequired(request.getInsuranceRequired() != null ? request.getInsuranceRequired() : Boolean.FALSE)
                .active(request.getActive() != null ? request.getActive() : Boolean.TRUE)
                .build();

        AssetType saved = assetTypeRepository.save(assetType);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AssetTypeResponse> listAll() {
        return assetTypeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssetTypeResponse getById(Long id) {
        AssetType at = assetTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset type not found"));
        return toResponse(at);
    }

    @Transactional
    public AssetTypeResponse update(Long id, AssetTypeUpdateRequest request) {
        AssetType existing = assetTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset type not found"));

        String code = normalize(request.getCode(), "Asset type code");
        String name = normalize(request.getName(), "Asset type name");

        if (assetTypeRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Asset type code already exists");
        }
        if (assetTypeRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Asset type name already exists");
        }

        AssetCategory category = null;
        if (request.getAssetCategoryId() != null) {
            category = assetCategoryService.getByIdOrThrow(request.getAssetCategoryId());
        }

        existing.setCode(code);
        existing.setName(name);
        existing.setAssetCategory(category);
        existing.setDefaultCriticality(request.getDefaultCriticality());
        existing.setDefaultGlAccount(trimToNull(request.getDefaultGlAccount()));
        existing.setInsuranceRequired(request.getInsuranceRequired() != null ? request.getInsuranceRequired() : Boolean.FALSE);
        existing.setActive(request.getActive() != null ? request.getActive() : Boolean.TRUE);

        AssetType saved = assetTypeRepository.save(existing);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        AssetType existing = assetTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset type not found"));

        if (assetRepository.existsByAssetTypeRef_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete: asset type is in use by assets");
        }

        assetTypeRepository.delete(existing);
    }

    private AssetTypeResponse toResponse(AssetType assetType) {
        return AssetTypeResponse.builder()
                .id(assetType.getId())
                .code(assetType.getCode())
                .name(assetType.getName())
                .assetCategoryId(assetType.getAssetCategory() != null ? assetType.getAssetCategory().getId() : null)
                .assetCategory(assetType.getAssetCategory() != null ? assetType.getAssetCategory().getName() : null)
                .defaultCriticality(assetType.getDefaultCriticality())
                .defaultGlAccount(assetType.getDefaultGlAccount())
                .insuranceRequired(assetType.getInsuranceRequired())
                .active(assetType.getActive())
                .build();
    }

    private String normalize(String raw, String fieldLabel) {
        if (raw == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldLabel + " is required");
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldLabel + " cannot be blank");
        }
        return trimmed;
    }

    private String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
