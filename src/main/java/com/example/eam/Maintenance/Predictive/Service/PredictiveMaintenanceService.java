package com.example.eam.Maintenance.Predictive.Service;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Asset.Repository.AssetRepository;
import com.example.eam.Enum.MeterType;
import com.example.eam.Enum.PriorityLevel;
import com.example.eam.Enum.WorkOrderSource;
import com.example.eam.Enum.WorkOrderStatus;
import com.example.eam.Enum.WorkType;
import com.example.eam.Maintenance.Predictive.Dto.AssetThresholdRequest;
import com.example.eam.Maintenance.Predictive.Dto.MeterReadingRequest;
import com.example.eam.Maintenance.Predictive.Entity.AssetThreshold;
import com.example.eam.Maintenance.Predictive.Repository.AssetThresholdRepository;
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
public class PredictiveMaintenanceService {

    private final AssetThresholdRepository thresholdRepository;
    private final AssetRepository assetRepository;
    private final WorkOrderRepository workOrderRepository;

    @Transactional
    public AssetThreshold upsertThreshold(@Valid AssetThresholdRequest req) {
        Asset asset = assetRepository.findById(req.getAssetId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset not found"));

        AssetThreshold threshold = thresholdRepository.findByAsset_IdAndMeterType(req.getAssetId(), req.getMeterType())
                .orElseGet(AssetThreshold::new);
        threshold.setAsset(asset);
        threshold.setMeterType(req.getMeterType());
        threshold.setWarningThreshold(req.getWarningThreshold());
        threshold.setCriticalThreshold(req.getCriticalThreshold());
        threshold.setAutoCreateWo(req.getAutoCreateWo() != null ? req.getAutoCreateWo() : true);
        threshold.setDefaultPriority(req.getDefaultPriority() != null ? req.getDefaultPriority() : PriorityLevel.MEDIUM);
        threshold.setCooldownHours(req.getCooldownHours() != null ? req.getCooldownHours() : 24);

        return thresholdRepository.save(threshold);
    }

    @Transactional
    public void recordMeterReading(@Valid MeterReadingRequest req) {
        Asset asset = assetRepository.findById(req.getAssetId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset not found"));

        AssetThreshold threshold = thresholdRepository.findByAsset_IdAndMeterType(req.getAssetId(), req.getMeterType())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No threshold configured for this meter type"));

        double value = req.getReadingValue();
        LocalDateTime now = req.getReadingTime() != null ? req.getReadingTime() : LocalDateTime.now();

        String severity = null;
        if (threshold.getCriticalThreshold() != null && value >= threshold.getCriticalThreshold()) {
            severity = "CRITICAL";
        } else if (threshold.getWarningThreshold() != null && value >= threshold.getWarningThreshold()) {
            severity = "WARNING";
        }

        if (severity == null) return;

        if (threshold.getLastTriggeredAt() != null && threshold.getCooldownHours() != null) {
            LocalDateTime nextAllowed = threshold.getLastTriggeredAt().plusHours(threshold.getCooldownHours());
            if (now.isBefore(nextAllowed)) {
                return;
            }
        }

        threshold.setLastTriggeredAt(now);
        threshold.setLastTriggeredSeverity(severity);
        thresholdRepository.save(threshold);

        if ("CRITICAL".equals(severity) && Boolean.TRUE.equals(threshold.getAutoCreateWo())) {
            createPredictiveWorkOrder(asset, req.getMeterType(), value);
        }
    }

    private void createPredictiveWorkOrder(Asset asset, MeterType meterType, double value) {
        WorkOrder wo = WorkOrder.builder()
                .workOrderId(generateUniqueWorkOrderId())
                .pmPlan(null)
                .pmDueDate(null)
                .asset(asset)
                .location(null)
                .workType(WorkType.PREDICTIVE)
                .priority(PriorityLevel.HIGH)
                .woTitle("Predictive alert: " + meterType)
                .descriptionScope("Meter " + meterType + " crossed threshold with value " + value)
                .status(WorkOrderStatus.NEW)
                .source(WorkOrderSource.PREDICTIVE)
                .deleted(false)
                .build();
        workOrderRepository.save(wo);
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
