package com.example.eam.Maintenance.Emergency.Service;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Asset.Entity.AssetLocation;
import com.example.eam.Asset.Repository.AssetLocationRepository;
import com.example.eam.Asset.Repository.AssetRepository;
import com.example.eam.Enum.PriorityLevel;
import com.example.eam.Enum.WorkOrderSource;
import com.example.eam.Enum.WorkOrderStatus;
import com.example.eam.Enum.WorkType;
import com.example.eam.Maintenance.Emergency.Dto.EmergencyIncidentListResponse;
import com.example.eam.Maintenance.Emergency.Dto.EmergencyIncidentResponse;
import com.example.eam.Maintenance.Emergency.Dto.EmergencyWorkOrderRequest;
import com.example.eam.Maintenance.Emergency.Entity.EmergencyIncident;
import com.example.eam.Maintenance.Emergency.Repository.EmergencyIncidentRepository;
import com.example.eam.WorkOrder.Dto.WorkOrderDetailsResponse;
import com.example.eam.WorkOrder.Entity.WorkOrder;
import com.example.eam.WorkOrder.Repository.WorkOrderRepository;
import com.example.eam.WorkOrder.Service.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class EmergencyMaintenanceService {

    private final WorkOrderRepository workOrderRepository;
    private final AssetRepository assetRepository;
    private final AssetLocationRepository assetLocationRepository;
    private final EmergencyIncidentRepository emergencyIncidentRepository;
    private final WorkOrderService workOrderService;

    @Transactional
    public WorkOrderDetailsResponse createEmergencyWo(@Valid EmergencyWorkOrderRequest req) {
        Asset asset = resolveAsset(req.getAssetId());
        String location = resolveLocation(asset, req.getLocation());
        if (asset == null && (location == null || location.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset or location is required");
        }

        WorkOrder wo = WorkOrder.builder()
                .workOrderId(generateUniqueWorkOrderId())
                .asset(asset)
                .location(location)
                .workType(WorkType.EMERGENCY)
                .priority(PriorityLevel.CRITICAL)
                .woTitle("Emergency WO")
                .descriptionScope(req.getFailureDescription())
                .failureDescription(req.getFailureDescription())
                .failureTime(req.getFailureTime() != null ? req.getFailureTime() : LocalDateTime.now())
                .downtimeStart(req.getFailureTime() != null ? req.getFailureTime() : LocalDateTime.now())
                .reporter(req.getReporter())
                .status(WorkOrderStatus.NEW)
                .source(WorkOrderSource.EMERGENCY)
                .targetCompletionDate(LocalDate.now())
                .deleted(false)
                .build();

        WorkOrder saved = workOrderRepository.save(wo);

        EmergencyIncident incident = EmergencyIncident.builder()
                .workOrder(saved)
                .asset(asset)
                .location(location)
                .failureDescription(req.getFailureDescription())
                .failureTime(wo.getFailureTime())
                .downtimeStart(wo.getDowntimeStart())
                .reporter(req.getReporter())
                .build();
        emergencyIncidentRepository.save(incident);

        return workOrderService.getWorkOrderDetails(saved.getId());
    }

    @Transactional(readOnly = true)
    public EmergencyIncidentResponse getIncident(Long id) {
        EmergencyIncident incident = getIncidentOrThrow(id);
        return mapToResponse(incident);
    }

    @Transactional(readOnly = true)
    public EmergencyIncidentListResponse listIncidents(Pageable pageable) {
        Page<EmergencyIncident> page = emergencyIncidentRepository.findAll(pageable);
        return EmergencyIncidentListResponse.builder()
                .incidents(page.getContent().stream().map(this::mapToResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private Asset resolveAsset(Long id) {
        if (id == null) return null;
        return assetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset not found"));
    }

    private String resolveLocation(Asset asset, String provided) {
        if (provided != null && !provided.trim().isEmpty()) return provided.trim();
        if (asset == null) return null;
        return assetLocationRepository.findByAsset_Id(asset.getId())
                .map(AssetLocation::getLocation)
                .orElse(null);
    }

    private String generateUniqueWorkOrderId() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // YYYYMMDD

        for (int attempt = 0; attempt < 30; attempt++) {
            int rand = ThreadLocalRandom.current().nextInt(0, 10000);
            String candidate = String.format("%s%04d", datePart, rand);

            if (!workOrderRepository.existsByWorkOrderId(candidate)) {
                return candidate;
            }
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Unable to generate unique Work Order ID");
    }

    private EmergencyIncident getIncidentOrThrow(Long id) {
        return emergencyIncidentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Emergency incident not found"));
    }

    private EmergencyIncidentResponse mapToResponse(EmergencyIncident incident) {
        WorkOrderDetailsResponse woDetails = resolveWorkOrderDetails(incident.getWorkOrder());
        return EmergencyIncidentResponse.builder()
                .id(incident.getId())
                .workOrderId(incident.getWorkOrder().getId())
                .workOrderNumber(incident.getWorkOrder().getWorkOrderId())
                .assetId(incident.getAsset() != null ? incident.getAsset().getId() : null)
                .assetName(incident.getAsset() != null ? incident.getAsset().getAssetName() : null)
                .location(incident.getLocation())
                .failureDescription(incident.getFailureDescription())
                .failureTime(incident.getFailureTime())
                .downtimeStart(incident.getDowntimeStart())
                .downtimeEnd(incident.getDowntimeEnd())
                .reporter(incident.getReporter())
                .createdAt(incident.getCreatedAt())
                .updatedAt(incident.getUpdatedAt())
                .workOrder(woDetails)
                .build();
    }

    private WorkOrderDetailsResponse resolveWorkOrderDetails(WorkOrder workOrder) {
        if (workOrder == null) return null;
        // avoid failing the entire response if the work order is soft-deleted or missing
        WorkOrder existing = workOrderRepository.findById(workOrder.getId()).orElse(null);
        if (existing == null || existing.isDeleted()) {
            return null;
        }
        try {
            return workOrderService.getWorkOrderDetails(existing.getId());
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            }
            throw ex;
        }
    }
}
