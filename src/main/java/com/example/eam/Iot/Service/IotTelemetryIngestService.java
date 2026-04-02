package com.example.eam.Iot.Service;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Asset.Repository.AssetRepository;
import com.example.eam.Enum.IngestProcessingStatus;
import com.example.eam.Iot.Dto.IotTelemetryBatchRequest;
import com.example.eam.Iot.Dto.IotTelemetryBatchResponse;
import com.example.eam.Iot.Dto.IotTelemetryReadingRequest;
import com.example.eam.Iot.Entity.IotDevice;
import com.example.eam.Iot.Entity.IotMetricCatalog;
import com.example.eam.Iot.Entity.IotTelemetryLog;
import com.example.eam.Iot.Repository.IotDeviceRepository;
import com.example.eam.Iot.Repository.IotTelemetryLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IotTelemetryIngestService {

    private static final int MAX_BATCH_SIZE = 500;

    private final IotDeviceRepository deviceRepository;
    private final IotTelemetryLogRepository telemetryLogRepository;
    private final IotMetricCatalogService metricCatalogService;
    private final AssetRepository assetRepository;
    private final IotAlertEngineService alertEngineService;
    private final IotSecurityService iotSecurityService;

    @Transactional
    public IotTelemetryBatchResponse ingest(String deviceUid,
                                            String timestampHeader,
                                            String nonceHeader,
                                            String signatureHeader,
                                            String rawBody,
                                            IotTelemetryBatchRequest request) {
        IotDevice device = deviceRepository.findByDeviceUid(deviceUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown device"));

        if (!device.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Device is disabled");
        }

        long timestamp = parseTimestamp(timestampHeader);
        if (!iotSecurityService.isTimestampWithinWindow(timestamp)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Request timestamp expired");
        }

        boolean signatureValid = iotSecurityService.verifySignature(
                deviceUid,
                timestampHeader,
                nonceHeader == null ? "" : nonceHeader,
                rawBody,
                signatureHeader,
                device.getSecretHash()
        );
        if (!signatureValid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid IoT signature");
        }

        if (request.getReadings().size() > MAX_BATCH_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 500 readings per request");
        }

        List<String> errors = new ArrayList<>();
        List<IotTelemetryLog> acceptedLogs = new ArrayList<>();
        int duplicateCount = 0;

        for (int i = 0; i < request.getReadings().size(); i++) {
            IotTelemetryReadingRequest row = request.getReadings().get(i);
            try {
                String eventId = normalizeOptional(row.getEventId());
                if (eventId != null && telemetryLogRepository
                        .findByCompanyIdAndDevice_IdAndEventId(device.getCompanyId(), device.getId(), eventId)
                        .isPresent()) {
                    duplicateCount++;
                    continue;
                }

                IotMetricCatalog metric = metricCatalogService.getByCodeOrThrow(device.getCompanyId(), row.getMetricCode());
                Asset asset = resolveAsset(device, row.getAssetId());
                LocalDateTime observedAt = row.getObservedAt() != null ? row.getObservedAt() : LocalDateTime.now();

                IotTelemetryLog log = IotTelemetryLog.builder()
                        .companyId(device.getCompanyId())
                        .device(device)
                        .asset(asset)
                        .metric(metric)
                        .metricCode(metric.getMetricCode())
                        .eventId(eventId)
                        .observedAt(observedAt)
                        .readingValue(row.getReadingValue())
                        .location(normalizeOptional(row.getLocation()))
                        .ingestStatus(IngestProcessingStatus.PENDING)
                        .processingAttempts(0)
                        .build();
                acceptedLogs.add(log);
            } catch (Exception ex) {
                errors.add("reading[" + i + "]: " + ex.getMessage());
            }
        }

        if (!acceptedLogs.isEmpty()) {
            telemetryLogRepository.saveAll(acceptedLogs);
            for (IotTelemetryLog log : acceptedLogs) {
                try {
                    alertEngineService.processTelemetryLog(log);
                } catch (Exception ex) {
                    errors.add("reading[id=" + log.getId() + "]: " + ex.getMessage());
                }
            }
            device.setLastSeenAt(LocalDateTime.now());
            deviceRepository.save(device);
        }

        int failedCount = errors.size();
        return IotTelemetryBatchResponse.builder()
                .receivedCount(request.getReadings().size())
                .acceptedCount(acceptedLogs.size())
                .duplicateCount(duplicateCount)
                .failedCount(failedCount)
                .errors(errors)
                .build();
    }

    @Transactional
    public int retryPendingAndFailed(int batchSize) {
        List<IotTelemetryLog> logs = telemetryLogRepository.findByIngestStatusInOrderByCreatedAtAsc(
                List.of(IngestProcessingStatus.PENDING, IngestProcessingStatus.FAILED),
                PageRequest.of(0, batchSize)
        );
        int processed = 0;
        for (IotTelemetryLog log : logs) {
            try {
                alertEngineService.processTelemetryLog(log);
                processed++;
            } catch (Exception ignored) {
                // errors are persisted by alertEngineService
            }
        }
        return processed;
    }

    @Transactional
    public long purgeOlderThanDays(int retentionDays) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        return telemetryLogRepository.deleteByObservedAtBefore(threshold);
    }

    private long parseTimestamp(String header) {
        if (header == null || header.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing IoT timestamp header");
        }
        try {
            return Long.parseLong(header.trim());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid IoT timestamp header");
        }
    }

    private Asset resolveAsset(IotDevice device, Long requestedAssetId) {
        Long companyId = device.getCompanyId();
        if (requestedAssetId != null) {
            return assetRepository.findByIdAndCompanyId(requestedAssetId, companyId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset not found"));
        }
        if (device.getAsset() == null) {
            return null;
        }
        return assetRepository.findByIdAndCompanyId(device.getAsset().getId(), companyId)
                .orElse(null);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

