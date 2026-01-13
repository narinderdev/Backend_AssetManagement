package com.example.eam.Maintenance.Emergency.Service;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Asset.Entity.AssetLocation;
import com.example.eam.Asset.Repository.AssetLocationRepository;
import com.example.eam.Asset.Repository.AssetRepository;
import com.example.eam.Enum.PriorityLevel;
import com.example.eam.Enum.WorkOrderSource;
import com.example.eam.Enum.WorkOrderStatus;
import com.example.eam.Enum.WorkType;
import com.example.eam.Maintenance.Emergency.Dto.EmergencyWorkOrderRequest;
import com.example.eam.Maintenance.Emergency.Entity.EmergencyIncident;
import com.example.eam.Maintenance.Emergency.Repository.EmergencyIncidentRepository;
import com.example.eam.WorkOrder.Entity.WorkOrder;
import com.example.eam.WorkOrder.Repository.WorkOrderRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public WorkOrder createEmergencyWo(@Valid EmergencyWorkOrderRequest req) {
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

        return saved;
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
            String candidate = String.format("WO-%s-%04d", datePart, rand);

            if (!workOrderRepository.existsByWorkOrderId(candidate)) {
                return candidate;
            }
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Unable to generate unique Work Order ID");
    }
}
