package com.example.eam.Iot.Service;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Asset.Repository.AssetRepository;
import com.example.eam.Common.CompanyContextHolder;
import com.example.eam.Iot.Dto.*;
import com.example.eam.Iot.Entity.IotDevice;
import com.example.eam.Iot.Repository.IotDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class IotDeviceService {

    private final IotDeviceRepository deviceRepository;
    private final AssetRepository assetRepository;
    private final IotSecurityService iotSecurityService;

    @Transactional
    public IotDeviceCreateResponse create(IotDeviceCreateRequest request) {
        Long companyId = requireCompanyId();
        String deviceUid = normalizeRequired(request.getDeviceUid(), "deviceUid");
        if (deviceRepository.existsByDeviceUid(deviceUid)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Device UID already exists");
        }

        Asset asset = resolveAsset(request.getAssetId(), companyId);
        String rawSecret = iotSecurityService.generateDeviceSecret();
        String secretHash = iotSecurityService.hashSecret(rawSecret);

        IotDevice saved = deviceRepository.save(IotDevice.builder()
                .companyId(companyId)
                .deviceUid(deviceUid)
                .deviceName(normalizeRequired(request.getDeviceName(), "deviceName"))
                .asset(asset)
                .location(normalizeOptional(request.getLocation()))
                .secretHash(secretHash)
                .enabled(request.getEnabled() == null || request.getEnabled())
                .build());

        return IotDeviceCreateResponse.builder()
                .device(toResponse(saved))
                .deviceSecret(rawSecret)
                .build();
    }

    @Transactional(readOnly = true)
    public IotDeviceResponse get(Long id) {
        Long companyId = requireCompanyId();
        return toResponse(getOrThrow(id, companyId));
    }

    @Transactional(readOnly = true)
    public Page<IotDeviceResponse> list(Pageable pageable) {
        Long companyId = requireCompanyId();
        return deviceRepository.findByCompanyId(companyId, pageable).map(this::toResponse);
    }

    @Transactional
    public IotDeviceResponse patch(Long id, IotDevicePatchRequest request) {
        Long companyId = requireCompanyId();
        IotDevice device = getOrThrow(id, companyId);

        if (request.getDeviceName() != null && !request.getDeviceName().isBlank()) {
            device.setDeviceName(request.getDeviceName().trim());
        }
        if (request.getLocation() != null) {
            device.setLocation(normalizeOptional(request.getLocation()));
        }
        if (request.getEnabled() != null) {
            device.setEnabled(request.getEnabled());
        }
        if (request.getAssetId() != null) {
            if (request.getAssetId() <= 0) {
                device.setAsset(null);
            } else {
                device.setAsset(resolveAsset(request.getAssetId(), companyId));
            }
        }
        return toResponse(deviceRepository.save(device));
    }

    @Transactional
    public IotRotateSecretResponse rotateSecret(Long id) {
        Long companyId = requireCompanyId();
        IotDevice device = getOrThrow(id, companyId);
        String rawSecret = iotSecurityService.generateDeviceSecret();
        device.setSecretHash(iotSecurityService.hashSecret(rawSecret));
        deviceRepository.save(device);
        return IotRotateSecretResponse.builder()
                .deviceId(device.getId())
                .deviceUid(device.getDeviceUid())
                .newSecret(rawSecret)
                .build();
    }

    private IotDevice getOrThrow(Long id, Long companyId) {
        return deviceRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "IoT device not found"));
    }

    private Asset resolveAsset(Long assetId, Long companyId) {
        if (assetId == null) {
            return null;
        }
        return assetRepository.findByIdAndCompanyId(assetId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset not found"));
    }

    private Long requireCompanyId() {
        return CompanyContextHolder.getCompanyId()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyId query parameter is required"));
    }

    private String normalizeRequired(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public IotDeviceResponse toResponse(IotDevice entity) {
        return IotDeviceResponse.builder()
                .id(entity.getId())
                .deviceUid(entity.getDeviceUid())
                .deviceName(entity.getDeviceName())
                .assetId(entity.getAsset() != null ? entity.getAsset().getId() : null)
                .assetName(entity.getAsset() != null ? entity.getAsset().getAssetName() : null)
                .location(entity.getLocation())
                .enabled(entity.isEnabled())
                .lastSeenAt(entity.getLastSeenAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

