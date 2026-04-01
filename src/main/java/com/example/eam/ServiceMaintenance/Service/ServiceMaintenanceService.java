package com.example.eam.ServiceMaintenance.Service;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Asset.Entity.AssetLocation;
import com.example.eam.Asset.Repository.AssetLocationRepository;
import com.example.eam.Asset.Repository.AssetRepository;
import com.example.eam.Common.CompanyContextHolder;
import com.example.eam.Enum.ServiceRequestStatus;
import com.example.eam.ServiceMaintenance.Dto.ServiceRequestApproveDto;
import com.example.eam.ServiceMaintenance.Dto.ServiceRequestCreateDto;
import com.example.eam.ServiceMaintenance.Dto.ServiceRequestRejectDto;
import com.example.eam.ServiceMaintenance.Dto.ServiceRequestResponse;
import com.example.eam.ServiceMaintenance.Dto.ServiceRequestUpdateDto;
import com.example.eam.ServiceMaintenance.Entity.ServiceMaintenance;
import com.example.eam.ServiceMaintenance.Repository.ServiceMaintenanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class ServiceMaintenanceService {

    private final ServiceMaintenanceRepository serviceRepo;
    private final AssetRepository assetRepository;
    private final AssetLocationRepository assetLocationRepository;
    

    // ---------- CREATE ----------

    @Transactional
    public ServiceRequestResponse create(ServiceRequestCreateDto dto) {
        Long companyId = CompanyContextHolder.getCompanyId().orElse(null);
        validatePreferredAssignment(dto.getPreferredTechnicianId(), dto.getPreferredTeamId());

        Asset asset = null;
        if (dto.getAssetId() != null) {
            asset = assetRepository.findByIdAndCompanyId(dto.getAssetId(), companyId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Asset not found: " + dto.getAssetId()
                    ));
        }

        // Resolve requestId (manual or auto)
        String requestId = resolveRequestIdOnCreate(dto.getRequestId(), companyId);

        String resolvedLocation = resolveLocation(dto.getLocation(), asset);

        ServiceMaintenance entity = ServiceMaintenance.builder()
                .companyId(companyId)
                .requestId(requestId)
                .requestDate(LocalDateTime.now())
                .requesterName(dto.getRequesterName())
                .requesterPhoneNumber(trim(dto.getRequesterPhoneNumber()))
                .requesterContact(dto.getRequesterContact())
                .department(dto.getDepartment())
                .asset(asset)
                .location(resolvedLocation)
                .maintenanceType(dto.getMaintenanceType())
                .priority(dto.getPriority())
                .shortTitle(dto.getShortTitle())
                .problemDescription(dto.getProblemDescription())
                .preferredStartDate(dto.getPreferredStartDate())
                .preferredStartTime(dto.getPreferredStartTime())
                .preferredEndDate(dto.getPreferredEndDate())
                .preferredEndTime(dto.getPreferredEndTime())
                .preferredTechnicianId(dto.getPreferredTechnicianId())
                .preferredTeamId(dto.getPreferredTeamId())
                .safetyRisk(dto.getSafetyRisk())
                .attachmentUrl(dto.getAttachmentUrl())
                .status(ServiceRequestStatus.NEW)
                .rejectionReason(null)
                .approvedBy(null)
                .approvedAt(null)
                .build();

        ServiceMaintenance saved = saveOrThrowConflict(entity);
        return toResponse(saved);
    }

    private String resolveRequestIdOnCreate(String userProvided, Long companyId) {
        if (userProvided != null && !userProvided.isBlank()) {
            String trimmed = userProvided.trim();
            if (serviceRepo.existsByRequestIdAndCompanyId(trimmed, companyId)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Service Request ID already exists: " + trimmed
                );
            }
            return trimmed;
        }

        // Auto-generate: SR-000001 style using max DB id
        return generateNextRequestId(companyId);
    }

    private String generateNextRequestId(Long companyId) {
        String prefix = "SR-";
        long base = serviceRepo.findTopByCompanyIdOrderByIdDesc(companyId)
                .map(ServiceMaintenance::getId)
                .orElse(0L);

        String candidate;
        do {
            base++;
            candidate = prefix + String.format("%06d", base);
        } while (serviceRepo.existsByRequestIdAndCompanyId(candidate, companyId));

        return candidate;
    }

    // ---------- READ SINGLE ----------

    @Transactional(readOnly = true)
    public ServiceRequestResponse get(Long id) {
        ServiceMaintenance entity = getOrThrow(id, CompanyContextHolder.getCompanyId().orElse(null));
        return toResponse(entity);
    }

    // ---------- LIST ----------

    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> list(Pageable pageable) {
        Long companyId = CompanyContextHolder.getCompanyId().orElse(null);
        return serviceRepo.findByDeletedFalseAndStatusNotAndCompanyId(ServiceRequestStatus.CONVERTED_TO_WO, companyId, pageable)
                .map(this::toResponse);
    }

    // ---------- UPDATE (PATCH) ----------

    @Transactional
    public ServiceRequestResponse update(Long id, ServiceRequestUpdateDto dto) {
        Long companyId = CompanyContextHolder.getCompanyId().orElse(null);
        ServiceMaintenance entity = getOrThrow(id, companyId);

        if (entity.getStatus() == ServiceRequestStatus.APPROVED
                || entity.getStatus() == ServiceRequestStatus.REJECTED
                || entity.getStatus() == ServiceRequestStatus.CONVERTED_TO_WO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot edit service request in status " + entity.getStatus());
        }

        // Request ID change (optional)
        if (dto.getRequestId() != null && !dto.getRequestId().isBlank()) {
            String newReqId = dto.getRequestId().trim();
            if (!newReqId.equals(entity.getRequestId())
                    && serviceRepo.existsByRequestIdAndCompanyId(newReqId, companyId)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Service Request ID already exists: " + newReqId
                );
            }
            entity.setRequestId(newReqId);
        }

        // Asset / Location
        if (dto.getAssetId() != null) {
            Asset asset = assetRepository.findByIdAndCompanyId(dto.getAssetId(), companyId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Asset not found: " + dto.getAssetId()
                    ));
            entity.setAsset(asset);

            if (dto.getLocation() == null || dto.getLocation().isBlank()) {
                String loc = resolveLocation(null, asset);
                if (loc != null) {
                    entity.setLocation(loc);
                }
            }
        }

        updateIfNotBlank(dto.getLocation(), entity::setLocation);
        updateIfNotBlank(dto.getRequesterName(), entity::setRequesterName);
        updateIfNotBlank(dto.getRequesterPhoneNumber(), entity::setRequesterPhoneNumber);
        updateIfNotBlank(dto.getRequesterContact(), entity::setRequesterContact);
        updateIfNotBlank(dto.getDepartment(), entity::setDepartment);
        updateIfNotBlank(dto.getShortTitle(), entity::setShortTitle);
        updateIfNotBlank(dto.getProblemDescription(), entity::setProblemDescription);
        updateIfNotBlank(dto.getAttachmentUrl(), entity::setAttachmentUrl);

        updateIfNotNull(dto.getMaintenanceType(), entity::setMaintenanceType);
        updateIfNotNull(dto.getPriority(), entity::setPriority);
        updateIfNotNull(dto.getPreferredStartDate(), entity::setPreferredStartDate);
        updateIfNotNull(dto.getPreferredStartTime(), entity::setPreferredStartTime);
        updateIfNotNull(dto.getPreferredEndDate(), entity::setPreferredEndDate);
        updateIfNotNull(dto.getPreferredEndTime(), entity::setPreferredEndTime);
        if (dto.getPreferredTechnicianId() != null || dto.getPreferredTeamId() != null) {
            validatePreferredAssignment(dto.getPreferredTechnicianId(), dto.getPreferredTeamId());
            // clear the other side if provided
            if (dto.getPreferredTechnicianId() != null) {
                entity.setPreferredTechnicianId(dto.getPreferredTechnicianId());
                entity.setPreferredTeamId(null);
            } else if (dto.getPreferredTeamId() != null) {
                entity.setPreferredTeamId(dto.getPreferredTeamId());
                entity.setPreferredTechnicianId(null);
            }
        }
        updateIfNotNull(dto.getSafetyRisk(), entity::setSafetyRisk);
        if (dto.getStatus() != null) {
            if (dto.getStatus() != ServiceRequestStatus.NEW && dto.getStatus() != ServiceRequestStatus.UNDER_REVIEW) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status can only be moved to NEW or UNDER_REVIEW via update");
            }
            entity.setStatus(dto.getStatus());
        }

        ServiceMaintenance saved = saveOrThrowConflict(entity);
        return toResponse(saved);
    }

    // ---------- DELETE ----------

    @Transactional
    public void delete(Long id) {
        ServiceMaintenance entity = getOrThrow(id, CompanyContextHolder.getCompanyId().orElse(null));
        entity.setDeleted(true);
        serviceRepo.save(entity);
    }

    // ---------- APPROVE / REJECT ----------

    @Transactional
    public ServiceRequestResponse approve(Long id, ServiceRequestApproveDto dto) {
        ServiceMaintenance entity = getOrThrow(id, CompanyContextHolder.getCompanyId().orElse(null));
        if (entity.getStatus() == ServiceRequestStatus.CONVERTED_TO_WO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Service request already converted to Work Order");
        }
        if (entity.getStatus() == ServiceRequestStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Service request already rejected");
        }
        entity.setStatus(ServiceRequestStatus.APPROVED);
        entity.setApprovedBy(dto != null ? trim(dto.getApprovedBy()) : null);
        entity.setApprovedAt(LocalDateTime.now());
        entity.setRejectionReason(null);
        entity.setDeleted(false);
        return toResponse(serviceRepo.save(entity));
    }

    @Transactional
    public ServiceRequestResponse reject(Long id, ServiceRequestRejectDto dto) {
        ServiceMaintenance entity = getOrThrow(id, CompanyContextHolder.getCompanyId().orElse(null));
        if (entity.getStatus() == ServiceRequestStatus.CONVERTED_TO_WO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Service request already converted to Work Order");
        }
        if (entity.getStatus() == ServiceRequestStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Approved request cannot be rejected");
        }
        String reason = (dto != null && dto.getReason() != null) ? dto.getReason().trim() : null;
        if (reason == null || reason.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rejection reason is required");
        }
        entity.setStatus(ServiceRequestStatus.REJECTED);
        entity.setRejectionReason(reason);
        entity.setApprovedBy(null);
        entity.setApprovedAt(null);
        entity.setDeleted(false);
        return toResponse(serviceRepo.save(entity));
    }

    // ---------- Helpers ----------

    private ServiceMaintenance getOrThrow(Long id, Long companyId) {
        return serviceRepo.findByIdAndDeletedFalseAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Service request not found: " + id
                ));
    }

    private String resolveLocation(String requestLocation, Asset asset) {
        if (requestLocation != null && !requestLocation.isBlank()) {
            return requestLocation.trim();
        }
        if (asset == null) return null;

        return assetLocationRepository.findByAsset_Id(asset.getId())
                .map(AssetLocation::getLocation)
                .orElse(null);
    }

    private <T> void updateIfNotNull(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private void updateIfNotBlank(String value, Consumer<String> setter) {
        if (value != null && !value.trim().isEmpty()) {
            setter.accept(value.trim());
        }
    }

    private ServiceRequestResponse toResponse(ServiceMaintenance entity) {
        Asset asset = entity.getAsset();

        return ServiceRequestResponse.builder()
                .id(entity.getId())
                .requestId(entity.getRequestId())
                .requestDate(entity.getRequestDate())
                .requesterName(entity.getRequesterName())
                .requesterPhoneNumber(entity.getRequesterPhoneNumber())
                .requesterContact(entity.getRequesterContact())
                .department(entity.getDepartment())
                .assetDbId(asset != null ? asset.getId() : null)
                .assetId(asset != null ? asset.getAssetId() : null)
                .assetName(asset != null ? asset.getAssetName() : null)
                .location(entity.getLocation())
                .maintenanceType(entity.getMaintenanceType())
                .priority(entity.getPriority())
                .shortTitle(entity.getShortTitle())
                .problemDescription(entity.getProblemDescription())
                .preferredStartDate(entity.getPreferredStartDate())
                .preferredStartTime(entity.getPreferredStartTime())
                .preferredEndDate(entity.getPreferredEndDate())
                .preferredEndTime(entity.getPreferredEndTime())
                .preferredTechnicianId(entity.getPreferredTechnicianId())
                .preferredTeamId(entity.getPreferredTeamId())
                .safetyRisk(entity.getSafetyRisk())
                .attachmentUrl(entity.getAttachmentUrl())
                .status(entity.getStatus())
                .approvedBy(entity.getApprovedBy())
                .approvedAt(entity.getApprovedAt())
                .rejectionReason(entity.getRejectionReason())
                .build();
    }

    private String trim(String val) {
        if (val == null) return null;
        String t = val.trim();
        return t.isEmpty() ? null : t;
    }

    private void validatePreferredAssignment(Long technicianId, Long teamId) {
        if (technicianId != null && teamId != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Specify either preferredTechnicianId or preferredTeamId, not both");
        }
    }

    private ServiceMaintenance saveOrThrowConflict(ServiceMaintenance entity) {
        try {
            return serviceRepo.save(entity);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Service Request ID already exists: " + entity.getRequestId(),
                    ex
            );
        }
    }
}
