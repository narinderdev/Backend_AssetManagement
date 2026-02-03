package com.example.eam.Maintenance.Predictive.Service;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Asset.Repository.AssetRepository;
import com.example.eam.Enum.MeterType;
import com.example.eam.Enum.PriorityLevel;
import com.example.eam.Enum.WorkOrderSource;
import com.example.eam.Enum.WorkOrderStatus;
import com.example.eam.Enum.WorkType;
import com.example.eam.Maintenance.Predictive.Dto.AssetThresholdRequest;
import com.example.eam.Maintenance.Predictive.Dto.AssetThresholdListResponse;
import com.example.eam.Maintenance.Predictive.Dto.AssetThresholdResponse;
import com.example.eam.Maintenance.Predictive.Dto.MeterReadingRequest;
import com.example.eam.Maintenance.Predictive.Dto.PredictiveMeterReadingResponse;
import com.example.eam.Maintenance.Predictive.Entity.AssetThreshold;
import com.example.eam.Maintenance.Predictive.Entity.PredictiveMeterReading;
import com.example.eam.Maintenance.Predictive.Repository.AssetThresholdRepository;
import com.example.eam.Maintenance.Predictive.Repository.PredictiveMeterReadingRepository;
import com.example.eam.WorkOrder.Entity.WorkOrder;
import com.example.eam.WorkOrder.Repository.WorkOrderRepository;
import com.example.eam.WorkOrder.Service.WoNumberPoolService;
import com.example.eam.WorkRequestType.Entity.WorkRequestType;
import com.example.eam.WorkRequestType.Service.WorkRequestTypeService;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class PredictiveMaintenanceService {

    private final AssetThresholdRepository thresholdRepository;
    private final AssetRepository assetRepository;
    private final WorkOrderRepository workOrderRepository;
    private final PredictiveMeterReadingRepository meterReadingRepository;
    private final WorkRequestTypeService workRequestTypeService;
    private final WoNumberPoolService woNumberPoolService;

    @Transactional
    public AssetThresholdResponse createThreshold(@Valid AssetThresholdRequest req) {
        Asset asset = assetRepository.findById(req.getAssetId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset not found"));

        thresholdRepository.findByAsset_Id(req.getAssetId())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Predictive threshold already exists for this asset");
                });

        String location = normalizeLocation(req.getLocation());

        AssetThreshold threshold = AssetThreshold.builder()
                .asset(asset)
                .location(location)
                .meterType(req.getMeterType())
                .warningThreshold(req.getWarningThreshold())
                .criticalThreshold(req.getCriticalThreshold())
                .autoCreateWo(req.getAutoCreateWo() != null ? req.getAutoCreateWo() : true)
                .defaultPriority(req.getDefaultPriority() != null ? req.getDefaultPriority() : PriorityLevel.MEDIUM)
                .cooldownHours(req.getCooldownHours() != null ? req.getCooldownHours() : 24)
                .build();

        AssetThreshold saved = thresholdRepository.save(threshold);
        List<PredictiveMeterReading> readings = meterReadingRepository.findByThreshold_Id(saved.getId());
        return toResponse(saved, readings);
    }

    @Transactional
    public AssetThresholdResponse updateThreshold(Long id, @Valid AssetThresholdRequest req) {
        AssetThreshold threshold = thresholdRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Threshold not found"));

        Asset asset = assetRepository.findById(req.getAssetId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset not found"));

        thresholdRepository.findByAsset_Id(req.getAssetId())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Predictive threshold already exists for this asset");
                });

        String location = normalizeLocation(req.getLocation());

        threshold.setAsset(asset);
        threshold.setLocation(location);
        threshold.setMeterType(req.getMeterType());
        threshold.setWarningThreshold(req.getWarningThreshold());
        threshold.setCriticalThreshold(req.getCriticalThreshold());
        threshold.setAutoCreateWo(req.getAutoCreateWo() != null ? req.getAutoCreateWo() : true);
        threshold.setDefaultPriority(req.getDefaultPriority() != null ? req.getDefaultPriority() : PriorityLevel.MEDIUM);
        threshold.setCooldownHours(req.getCooldownHours() != null ? req.getCooldownHours() : 24);

        AssetThreshold saved = thresholdRepository.save(threshold);
        List<PredictiveMeterReading> readings = meterReadingRepository.findByThreshold_Id(saved.getId());
        return toResponse(saved, readings);
    }

    @Transactional(readOnly = true)
    public AssetThresholdResponse getThreshold(Long id) {
        AssetThreshold threshold = thresholdRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Threshold not found"));
        List<PredictiveMeterReading> readings = meterReadingRepository.findByThreshold_Id(threshold.getId());
        return toResponse(threshold, readings);
    }

    @Transactional(readOnly = true)
    public AssetThresholdListResponse listThresholds(Pageable pageable) {
        Page<AssetThreshold> page = thresholdRepository.findAll(pageable);
        List<Long> ids = page.getContent().stream().map(AssetThreshold::getId).toList();
        Map<Long, List<PredictiveMeterReading>> readingsByThreshold = meterReadingRepository.findByThreshold_IdIn(ids).stream()
                .collect(Collectors.groupingBy(reading -> reading.getThreshold().getId()));

        List<AssetThresholdResponse> rows = page.getContent().stream()
                .map(th -> toResponse(th, readingsByThreshold.getOrDefault(th.getId(), List.of())))
                .toList();

        return AssetThresholdListResponse.builder()
                .thresholds(rows)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional
    public void deleteThreshold(Long id) {
        AssetThreshold threshold = thresholdRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Threshold not found"));
        meterReadingRepository.deleteByThreshold_Id(threshold.getId());
        thresholdRepository.delete(threshold);
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

        PredictiveMeterReading reading = PredictiveMeterReading.builder()
                .threshold(threshold)
                .asset(asset)
                .meterType(req.getMeterType())
                .readingValue(value)
                .readingTime(now)
                .severity(severity)
                .notes(req.getNotes())
                .build();
        meterReadingRepository.save(reading);

        if (severity == null) return;

        boolean skipCooldown = MeterType.TEMPERATURE.equals(threshold.getMeterType());
        if (!skipCooldown && threshold.getLastTriggeredAt() != null && threshold.getCooldownHours() != null) {
            LocalDateTime nextAllowed = threshold.getLastTriggeredAt().plusHours(threshold.getCooldownHours());
            if (now.isBefore(nextAllowed)) {
                return;
            }
        }

        threshold.setLastTriggeredAt(now);
        threshold.setLastTriggeredSeverity(severity);
        thresholdRepository.save(threshold);

        if ("CRITICAL".equals(severity) && Boolean.TRUE.equals(threshold.getAutoCreateWo())) {
            createPredictiveWorkOrder(asset, threshold, req.getMeterType(), value);
        }
    }

      private void createPredictiveWorkOrder(Asset asset, AssetThreshold threshold, MeterType meterType, double value) {
          PriorityLevel priority = threshold.getDefaultPriority() != null ? threshold.getDefaultPriority() : PriorityLevel.HIGH;
          String location = resolveLocation(asset, threshold.getLocation());
          WorkRequestType workRequestType = workRequestTypeService.getOrCreateDefaultExpenseType();
          WorkOrder wo = WorkOrder.builder()
                  .workOrderId(generateUniqueWorkOrderId())
                  .pmPlan(null)
                  .pmDueDate(null)
                  .asset(asset)
                  .workRequestType(workRequestType)
                  .location(location)
                  .workType(WorkType.PREDICTIVE)
                  .priority(priority)
                  .woTitle("Predictive alert: " + meterType)
                  .descriptionScope("Meter " + meterType + " crossed threshold with value " + value)
                  .status(WorkOrderStatus.NEW)
                  .source(WorkOrderSource.PREDICTIVE)
                  .deleted(false)
                  .build();
          WorkOrder saved = workOrderRepository.save(wo);
          String woNumber = woNumberPoolService.allocateWoNumber(saved.getId());
          saved.setWoNumber(woNumber);
          workOrderRepository.save(saved);
      }

    private String normalizeLocation(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveLocation(Asset asset, String providedLocation) {
        String normalized = normalizeLocation(providedLocation);
        if (normalized != null) {
            return normalized;
        }
        if (asset == null || asset.getLocation() == null) {
            return null;
        }
        return normalizeLocation(asset.getLocation().getLocation());
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

    private AssetThresholdResponse toResponse(AssetThreshold threshold, List<PredictiveMeterReading> readings) {
        return AssetThresholdResponse.builder()
                .id(threshold.getId())
                .assetId(threshold.getAsset() != null ? threshold.getAsset().getId() : null)
                .assetName(threshold.getAsset() != null ? threshold.getAsset().getAssetName() : null)
                .location(threshold.getLocation())
                .meterType(threshold.getMeterType())
                .warningThreshold(threshold.getWarningThreshold())
                .criticalThreshold(threshold.getCriticalThreshold())
                .autoCreateWo(threshold.getAutoCreateWo())
                .defaultPriority(threshold.getDefaultPriority())
                .cooldownHours(threshold.getCooldownHours())
                .lastTriggeredSeverity(threshold.getLastTriggeredSeverity())
                .meterReadings(readings.stream()
                        .map(r -> PredictiveMeterReadingResponse.builder()
                                .id(r.getId())
                                .meterType(r.getMeterType())
                                .readingValue(r.getReadingValue())
                                .readingTime(r.getReadingTime())
                                .severity(r.getSeverity())
                                .notes(r.getNotes())
                                .createdAt(r.getCreatedAt())
                                .build())
                        .toList())
                .build();
    }
}
