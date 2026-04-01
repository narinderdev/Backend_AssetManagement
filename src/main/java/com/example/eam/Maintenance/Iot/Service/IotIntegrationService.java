package com.example.eam.Maintenance.Iot.Service;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Asset.Repository.AssetLocationRepository;
import com.example.eam.Asset.Repository.AssetRepository;
import com.example.eam.Common.CompanyContextHolder;
import com.example.eam.Enum.MaintenanceType;
import com.example.eam.Enum.MeterType;
import com.example.eam.Enum.RequestPriority;
import com.example.eam.Enum.ServiceRequestStatus;
import com.example.eam.Maintenance.Iot.Dto.*;
import com.example.eam.Maintenance.Iot.Entity.IotAlert;
import com.example.eam.Maintenance.Iot.Entity.IotAlertAction;
import com.example.eam.Maintenance.Iot.Entity.IotDevice;
import com.example.eam.Maintenance.Iot.Entity.IotTelemetryLog;
import com.example.eam.Maintenance.Iot.Enum.IotAlertSeverity;
import com.example.eam.Maintenance.Iot.Enum.IotAlertStatus;
import com.example.eam.Maintenance.Iot.Enum.IotAlertType;
import com.example.eam.Maintenance.Iot.Repository.IotAlertActionRepository;
import com.example.eam.Maintenance.Iot.Repository.IotAlertRepository;
import com.example.eam.Maintenance.Iot.Repository.IotDeviceRepository;
import com.example.eam.Maintenance.Iot.Repository.IotTelemetryLogRepository;
import com.example.eam.Maintenance.Predictive.Entity.AssetThreshold;
import com.example.eam.Maintenance.Predictive.Repository.AssetThresholdRepository;
import com.example.eam.ServiceMaintenance.Entity.ServiceMaintenance;
import com.example.eam.ServiceMaintenance.Repository.ServiceMaintenanceRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class IotIntegrationService {
    private static final Logger log = LoggerFactory.getLogger(IotIntegrationService.class);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String SYSTEM_REQUESTER = "IoT Monitoring";
    private static final EnumSet<IotAlertSeverity> ABNORMAL_SEVERITIES =
            EnumSet.of(IotAlertSeverity.MEDIUM, IotAlertSeverity.HIGH, IotAlertSeverity.CRITICAL);

    private final IotDeviceRepository iotDeviceRepository;
    private final IotTelemetryLogRepository iotTelemetryLogRepository;
    private final IotAlertRepository iotAlertRepository;
    private final IotAlertActionRepository iotAlertActionRepository;
    private final AssetRepository assetRepository;
    private final AssetLocationRepository assetLocationRepository;
    private final AssetThresholdRepository assetThresholdRepository;
    private final ServiceMaintenanceRepository serviceMaintenanceRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public IotDeviceResponse createDevice(@Valid IotDeviceCreateRequest req) {
        Long companyId = requireCompanyContext();
        String deviceUid = normalizeRequired(req.getDeviceUid(), "deviceUid");
        String deviceName = normalizeRequired(req.getDeviceName(), "deviceName");

        iotDeviceRepository.findByCompanyIdAndDeviceUid(companyId, deviceUid)
                .ifPresent(existing -> {
                    log.warn("IoT device registration rejected: companyId={}, deviceUid={} already exists", companyId, deviceUid);
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Device UID already registered");
                });

        Asset asset = resolveAssetForCompany(req.getAssetId(), companyId);
        String location = resolveLocation(asset, req.getLocation());
        String plainToken = normalize(req.getAuthToken());
        if (plainToken == null) {
            plainToken = generateProvisioningToken();
        }

        IotDevice device = IotDevice.builder()
                .companyId(companyId)
                .deviceUid(deviceUid)
                .deviceName(deviceName)
                .asset(asset)
                .location(location)
                .enabled(req.getEnabled() == null || req.getEnabled())
                .authTokenHash(passwordEncoder.encode(plainToken))
                .build();

        IotDevice saved = iotDeviceRepository.save(device);
        log.info("IoT device registered: companyId={}, deviceId={}, deviceUid={}, deviceName={}, assetId={}, enabled={}",
                companyId, saved.getId(), saved.getDeviceUid(), saved.getDeviceName(),
                saved.getAsset() != null ? saved.getAsset().getId() : null, saved.getEnabled());
        return toDeviceResponse(saved, plainToken, 10);
    }

    @Transactional
    public IotDeviceResponse updateDevice(Long id, IotDeviceUpdateRequest req) {
        Long companyId = requireCompanyContext();
        IotDevice device = getDeviceOrThrow(id, companyId);

        if (req.getDeviceName() != null) {
            device.setDeviceName(normalizeRequired(req.getDeviceName(), "deviceName"));
        }
        if (req.getAssetId() != null) {
            device.setAsset(resolveAssetForCompany(req.getAssetId(), companyId));
        }
        if (req.getLocation() != null) {
            device.setLocation(normalize(req.getLocation()));
        } else if (req.getAssetId() != null && device.getAsset() != null) {
            device.setLocation(resolveLocation(device.getAsset(), null));
        }
        if (req.getEnabled() != null) {
            device.setEnabled(req.getEnabled());
        }

        String plainToken = null;
        boolean rotate = Boolean.TRUE.equals(req.getRotateToken());
        String suppliedToken = normalize(req.getAuthToken());
        if (rotate || suppliedToken != null) {
            plainToken = suppliedToken != null ? suppliedToken : generateProvisioningToken();
            device.setAuthTokenHash(passwordEncoder.encode(plainToken));
        }

        IotDevice saved = iotDeviceRepository.save(device);
        log.info("IoT device updated: companyId={}, deviceId={}, deviceUid={}, assetId={}, enabled={}, tokenRotated={}",
                companyId, saved.getId(), saved.getDeviceUid(),
                saved.getAsset() != null ? saved.getAsset().getId() : null,
                saved.getEnabled(), plainToken != null);
        return toDeviceResponse(saved, plainToken, 10);
    }

    @Transactional(readOnly = true)
    public IotDeviceListResponse listDevices(Pageable pageable, Integer offlineAfterMinutes) {
        Long companyId = requireCompanyContext();
        Page<IotDevice> page = iotDeviceRepository.findByCompanyId(companyId, pageable);
        int offlineMins = normalizeOfflineWindow(offlineAfterMinutes);
        List<IotDeviceResponse> rows = page.getContent().stream()
                .map(device -> toDeviceResponse(device, null, offlineMins))
                .toList();

        IotDeviceListResponse response = IotDeviceListResponse.builder()
                .devices(rows)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
        log.info("IoT devices listed: companyId={}, page={}, size={}, returned={}",
                companyId, response.getPage(), response.getSize(), response.getDevices().size());
        return response;
    }

    @Transactional
    public void deleteDevice(Long id) {
        Long companyId = requireCompanyContext();
        IotDevice device = getDeviceOrThrow(id, companyId);
        device.setEnabled(false);
        iotDeviceRepository.save(device);
        log.info("IoT device disabled: companyId={}, deviceId={}, deviceUid={}", companyId, device.getId(), device.getDeviceUid());
    }

    @Transactional(readOnly = true)
    public IotDashboardResponse getDashboard(Long assetId, String location, IotAlertSeverity severity, Integer offlineAfterMinutes) {
        Long companyId = requireCompanyContext();
        int offlineMins = normalizeOfflineWindow(offlineAfterMinutes);
        String normalizedLocation = normalize(location);

        List<IotDeviceResponse> devices = iotDeviceRepository.findByCompanyId(companyId).stream()
                .filter(d -> assetId == null || (d.getAsset() != null && assetId.equals(d.getAsset().getId())))
                .filter(d -> normalizedLocation == null || normalizedLocation.equalsIgnoreCase(normalize(d.getLocation())))
                .map(d -> toDeviceResponse(d, null, offlineMins))
                .toList();

        List<IotAlertResponse> activeAlerts = iotAlertRepository.findFiltered(
                        companyId,
                        assetId,
                        normalizedLocation,
                        severity,
                        IotAlertStatus.OPEN,
                        PageRequest.of(0, 30)
                ).stream()
                .map(this::toAlertResponse)
                .toList();

        List<IotAlertResponse> recentIssues = iotAlertRepository.findFiltered(
                        companyId,
                        assetId,
                        normalizedLocation,
                        severity,
                        null,
                        PageRequest.of(0, 30)
                ).stream()
                .map(this::toAlertResponse)
                .toList();

        long registeredDeviceCount = iotDeviceRepository.countByCompanyId(companyId);
        long onlineDeviceCount = iotDeviceRepository.countByCompanyIdAndEnabledTrueAndLastSeenAtAfter(
                companyId,
                LocalDateTime.now().minusMinutes(offlineMins)
        );
        long activeAlertCount = iotAlertRepository.countByCompanyIdAndStatus(companyId, IotAlertStatus.OPEN);

        IotDashboardResponse response = IotDashboardResponse.builder()
                .registeredDeviceCount(registeredDeviceCount)
                .onlineDeviceCount(onlineDeviceCount)
                .activeAlertCount(activeAlertCount)
                .deviceStatus(devices)
                .activeAlerts(activeAlerts)
                .recentIssues(recentIssues)
                .build();
        log.info("IoT dashboard fetched: companyId={}, assetId={}, location={}, severity={}, devices={}, activeAlerts={}, recentIssues={}",
                companyId, assetId, normalizedLocation, severity,
                response.getDeviceStatus().size(), response.getActiveAlerts().size(), response.getRecentIssues().size());
        return response;
    }

    @Transactional
    public IotAlertResponse acknowledgeAlert(Long id, IotAlertActionRequest req) {
        Long companyId = requireCompanyContext();
        IotAlert alert = getAlertOrThrow(id, companyId);
        if (alert.getStatus() == IotAlertStatus.RESOLVED) {
            log.warn("IoT alert acknowledge rejected: companyId={}, alertId={} already resolved", companyId, id);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Resolved alert cannot be acknowledged");
        }
        alert.setStatus(IotAlertStatus.ACKNOWLEDGED);
        IotAlert saved = iotAlertRepository.save(alert);
        logAlertAction(saved, "ALERT_ACKNOWLEDGED", req != null ? req.getActionBy() : null, req != null ? req.getNotes() : null);
        log.info("IoT alert acknowledged: companyId={}, alertId={}, severity={}, type={}",
                companyId, saved.getId(), saved.getSeverity(), saved.getAlertType());
        return toAlertResponse(saved);
    }

    @Transactional
    public IotAlertResponse resolveAlert(Long id, IotAlertActionRequest req) {
        Long companyId = requireCompanyContext();
        IotAlert alert = getAlertOrThrow(id, companyId);
        alert.setStatus(IotAlertStatus.RESOLVED);
        IotAlert saved = iotAlertRepository.save(alert);
        logAlertAction(saved, "ALERT_RESOLVED", req != null ? req.getActionBy() : null, req != null ? req.getNotes() : null);
        log.info("IoT alert resolved: companyId={}, alertId={}, severity={}, type={}",
                companyId, saved.getId(), saved.getSeverity(), saved.getAlertType());
        return toAlertResponse(saved);
    }

    @Transactional
    public List<IotTelemetryIngestResponse> generateSampleData(IotSampleDataRequest req) {
        Long companyId = requireCompanyContext();
        IotDevice device = iotDeviceRepository.findByCompanyIdAndDeviceUid(companyId, normalizeRequired(req.getDeviceUid(), "deviceUid"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        if (!Boolean.TRUE.equals(device.getEnabled())) {
            log.warn("IoT sample generation rejected: companyId={}, deviceUid={} disabled", companyId, device.getDeviceUid());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Device is disabled");
        }

        AssetThreshold threshold = device.getAsset() == null ? null :
                assetThresholdRepository.findByAsset_IdAndMeterTypeAndAsset_CompanyId(device.getAsset().getId(), req.getMeterType(), companyId).orElse(null);

        int count = req.getCount() == null ? 10 : Math.min(Math.max(req.getCount(), 1), 100);
        List<IotTelemetryIngestResponse> responses = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            double value = generateSampleValue(i, threshold);
            responses.add(processTelemetry(
                    device,
                    req.getMeterType(),
                    value,
                    LocalDateTime.now().minusMinutes(count - i),
                    null,
                    "sample-data"
            ));
        }
        log.info("IoT sample telemetry generated: companyId={}, deviceUid={}, meterType={}, count={}",
                companyId, device.getDeviceUid(), req.getMeterType(), responses.size());
        return responses;
    }

    @Transactional
    public IotTelemetryIngestResponse ingestTelemetry(@Valid IotTelemetryIngestRequest req) {
        Long companyId = req.getCompanyId();
        if (companyId == null || companyId <= 0) {
            log.warn("IoT ingest rejected: invalid companyId={}", companyId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyId is required");
        }
        String deviceUid = normalizeRequired(req.getDeviceUid(), "deviceUid");
        IotDevice device = iotDeviceRepository.findByCompanyIdAndDeviceUid(companyId, deviceUid)
                .orElseThrow(() -> {
                    log.warn("IoT ingest rejected: companyId={}, deviceUid={} not registered", companyId, deviceUid);
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unrecognized device");
                });

        if (!Boolean.TRUE.equals(device.getEnabled())) {
            log.warn("IoT ingest rejected: companyId={}, deviceUid={} disabled", companyId, deviceUid);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Device is disabled");
        }

        if (!passwordEncoder.matches(req.getAuthToken(), device.getAuthTokenHash())) {
            log.warn("IoT ingest rejected: companyId={}, deviceUid={} invalid credentials", companyId, deviceUid);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid device credentials");
        }

        log.info("IoT ingest accepted: companyId={}, deviceUid={}, meterType={}, readingValue={}, readingTime={}",
                companyId, device.getDeviceUid(), req.getMeterType(), req.getReadingValue(), req.getReadingTime());

        return processTelemetry(
                device,
                req.getMeterType(),
                req.getReadingValue(),
                req.getReadingTime(),
                req.getLocation(),
                req.getNotes()
        );
    }

    private IotTelemetryIngestResponse processTelemetry(
            IotDevice device,
            MeterType meterType,
            Double readingValue,
            LocalDateTime readingTime,
            String location,
            String notes
    ) {
        if (meterType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "meterType is required");
        }
        if (readingValue == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "readingValue is required");
        }

        LocalDateTime now = readingTime != null ? readingTime : LocalDateTime.now();
        Long companyId = device.getCompanyId();
        Asset asset = device.getAsset();
        AssetThreshold threshold = asset == null ? null :
                assetThresholdRepository.findByAsset_IdAndMeterTypeAndAsset_CompanyId(asset.getId(), meterType, companyId).orElse(null);

        IotAlertSeverity baseSeverity = classifyBaseSeverity(readingValue, threshold);
        boolean suddenSpike = isSuddenSpike(device, meterType, readingValue, now, threshold);
        boolean continuousAbnormal = isContinuousAbnormal(device, meterType, now, threshold, baseSeverity);

        IotAlertSeverity finalSeverity = baseSeverity;
        IotAlertType alertType = baseSeverity == IotAlertSeverity.LOW ? null : IotAlertType.THRESHOLD_BREACH;

        if (suddenSpike) {
            finalSeverity = maxSeverity(finalSeverity, IotAlertSeverity.HIGH);
            alertType = IotAlertType.SUDDEN_SPIKE;
        }
        if (continuousAbnormal) {
            finalSeverity = IotAlertSeverity.CRITICAL;
            alertType = IotAlertType.CONTINUOUS_ABNORMAL;
        }
        log.info("IoT telemetry classified: companyId={}, deviceUid={}, meterType={}, readingValue={}, severity={}, alertType={}, suddenSpike={}, continuousAbnormal={}",
                companyId, device.getDeviceUid(), meterType, readingValue, finalSeverity, alertType, suddenSpike, continuousAbnormal);

        String resolvedLocation = normalize(location);
        if (resolvedLocation == null) {
            resolvedLocation = resolveLocation(asset, device.getLocation());
        }

        IotTelemetryLog telemetry = IotTelemetryLog.builder()
                .companyId(companyId)
                .device(device)
                .asset(asset)
                .meterType(meterType)
                .readingValue(readingValue)
                .readingTime(now)
                .severity(finalSeverity)
                .anomalyType(alertType)
                .notes(normalize(notes))
                .build();
        iotTelemetryLogRepository.save(telemetry);

        device.setLastSeenAt(now);
        if (normalize(location) != null) {
            device.setLocation(normalize(location));
        }
        iotDeviceRepository.save(device);

        Long alertId = null;
        Long serviceRequestId = null;
        if (ABNORMAL_SEVERITIES.contains(finalSeverity)) {
            IotAlert alert = createOrReuseOpenAlert(device, asset, meterType, readingValue, threshold, finalSeverity, alertType, resolvedLocation, now);
            alertId = alert.getId();

            if (alert.getServiceRequest() == null) {
                ServiceMaintenance sr = createAutoServiceRequest(alert, finalSeverity, readingValue);
                alert.setServiceRequest(sr);
                IotAlert saved = iotAlertRepository.save(alert);
                logAlertAction(saved, "SERVICE_REQUEST_CREATED", SYSTEM_REQUESTER,
                        "Auto-created service request " + sr.getRequestId());
                serviceRequestId = sr.getId();
                log.info("IoT auto service request linked: companyId={}, alertId={}, serviceRequestId={}, requestCode={}",
                        companyId, saved.getId(), sr.getId(), sr.getRequestId());
            } else {
                serviceRequestId = alert.getServiceRequest().getId();
            }
        } else {
            log.info("IoT telemetry normal: companyId={}, deviceUid={}, meterType={}, readingValue={}",
                    companyId, device.getDeviceUid(), meterType, readingValue);
        }

        return IotTelemetryIngestResponse.builder()
                .processedAt(now)
                .severity(finalSeverity)
                .alertType(alertType)
                .anomalyDetected(suddenSpike || continuousAbnormal)
                .alertId(alertId)
                .serviceRequestId(serviceRequestId)
                .message(buildResultMessage(finalSeverity, suddenSpike, continuousAbnormal))
                .build();
    }

    private IotAlert createOrReuseOpenAlert(
            IotDevice device,
            Asset asset,
            MeterType meterType,
            Double readingValue,
            AssetThreshold threshold,
            IotAlertSeverity severity,
            IotAlertType alertType,
            String location,
            LocalDateTime occurredAt
    ) {
        int cooldownHours = threshold != null && threshold.getCooldownHours() != null
                ? Math.max(threshold.getCooldownHours(), 1)
                : 4;

        LocalDateTime fromTs = occurredAt.minusHours(cooldownHours);
        List<IotAlert> recentOpen = iotAlertRepository.findRecentByDeviceAndMeterAndStatus(
                device.getCompanyId(),
                device.getId(),
                meterType,
                IotAlertStatus.OPEN,
                fromTs
        );
        if (!recentOpen.isEmpty()) {
            IotAlert existing = recentOpen.get(0);
            log.info("IoT alert reused within cooldown: companyId={}, alertId={}, deviceUid={}, meterType={}, severity={}",
                    device.getCompanyId(), existing.getId(), device.getDeviceUid(), meterType, existing.getSeverity());
            return existing;
        }

        IotAlert alert = IotAlert.builder()
                .companyId(device.getCompanyId())
                .device(device)
                .asset(asset)
                .meterType(meterType)
                .readingValue(readingValue)
                .thresholdValue(resolveThresholdValue(threshold, severity))
                .severity(severity)
                .alertType(alertType != null ? alertType : IotAlertType.THRESHOLD_BREACH)
                .status(IotAlertStatus.OPEN)
                .location(location)
                .message(buildAlertMessage(device, meterType, readingValue, severity, alertType))
                .occurredAt(occurredAt)
                .build();
        IotAlert saved = iotAlertRepository.save(alert);
        logAlertAction(saved, "ALERT_CREATED", SYSTEM_REQUESTER, saved.getMessage());
        log.info("IoT alert created: companyId={}, alertId={}, deviceUid={}, meterType={}, severity={}, type={}",
                device.getCompanyId(), saved.getId(), device.getDeviceUid(), meterType, severity, saved.getAlertType());
        return saved;
    }

    private ServiceMaintenance createAutoServiceRequest(IotAlert alert, IotAlertSeverity severity, Double readingValue) {
        Asset asset = alert.getAsset();
        LocalDateTime now = LocalDateTime.now();
        MaintenanceType maintenanceType = severity == IotAlertSeverity.CRITICAL ? MaintenanceType.EMERGENCY : MaintenanceType.PREDICTIVE;

        String assetName = asset != null ? normalize(asset.getAssetName()) : null;
        String meter = alert.getMeterType() != null ? alert.getMeterType().name() : "UNKNOWN";
        String titleAsset = assetName != null ? assetName : "Unmapped Asset";

        for (int attempt = 0; attempt < 5; attempt++) {
            String requestId = generateNextServiceRequestId(alert.getCompanyId());

            ServiceMaintenance entity = ServiceMaintenance.builder()
                    .companyId(alert.getCompanyId())
                    .requestId(requestId)
                    .linkedWorkOrderId(null)
                    .requestDate(now)
                    .requesterName(SYSTEM_REQUESTER)
                    .requesterContact("device:" + alert.getDevice().getDeviceUid())
                    .department("IoT Monitoring")
                    .asset(asset)
                    .location(alert.getLocation())
                    .maintenanceType(maintenanceType)
                    .priority(mapSeverityToRequestPriority(severity))
                    .shortTitle("IoT Alert - " + meter + " - " + titleAsset)
                    .problemDescription(buildServiceRequestDescription(alert, readingValue))
                    .preferredStartDate(null)
                    .preferredStartTime(null)
                    .preferredEndDate(null)
                    .preferredEndTime(null)
                    .preferredTechnicianId(null)
                    .preferredTeamId(null)
                    .safetyRisk(severity == IotAlertSeverity.CRITICAL)
                    .attachmentUrl(null)
                    .status(ServiceRequestStatus.NEW)
                    .approvedBy(null)
                    .approvedAt(null)
                    .rejectionReason(null)
                    .deleted(false)
                    .build();

            try {
                ServiceMaintenance saved = serviceMaintenanceRepository.save(entity);
                log.info("IoT service request created: companyId={}, requestId={}, severity={}, maintenanceType={}, assetId={}",
                        alert.getCompanyId(), saved.getRequestId(), severity, maintenanceType,
                        asset != null ? asset.getId() : null);
                return saved;
            } catch (DataIntegrityViolationException ex) {
                if (attempt == 4) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to allocate unique service request ID", ex);
                }
            }
        }

        throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to allocate unique service request ID");
    }

    private String generateNextServiceRequestId(Long companyId) {
        String prefix = "SR-";
        long base = serviceMaintenanceRepository.findTopByCompanyIdOrderByIdDesc(companyId)
                .map(ServiceMaintenance::getId)
                .orElse(0L);
        String candidate;
        do {
            base++;
            candidate = prefix + String.format("%06d", base);
        } while (serviceMaintenanceRepository.existsByRequestIdAndCompanyId(candidate, companyId));
        return candidate;
    }

    private IotAlertSeverity classifyBaseSeverity(Double value, AssetThreshold threshold) {
        if (threshold == null) {
            return IotAlertSeverity.LOW;
        }
        Double warning = threshold.getWarningThreshold();
        Double critical = threshold.getCriticalThreshold();
        if (critical != null && value >= critical) {
            if (value >= critical * 1.2d) {
                return IotAlertSeverity.CRITICAL;
            }
            return IotAlertSeverity.HIGH;
        }
        if (warning != null && value >= warning) {
            return IotAlertSeverity.MEDIUM;
        }
        return IotAlertSeverity.LOW;
    }

    private boolean isSuddenSpike(IotDevice device, MeterType meterType, Double value, LocalDateTime now, AssetThreshold threshold) {
        IotTelemetryLog previous = iotTelemetryLogRepository
                .findTopByDevice_IdAndMeterTypeAndReadingTimeBeforeOrderByReadingTimeDesc(device.getId(), meterType, now)
                .orElse(null);
        if (previous == null || previous.getReadingValue() == null) {
            return false;
        }

        double baseline = threshold != null && threshold.getCriticalThreshold() != null
                ? Math.abs(threshold.getCriticalThreshold())
                : threshold != null && threshold.getWarningThreshold() != null
                ? Math.abs(threshold.getWarningThreshold())
                : Math.max(Math.abs(previous.getReadingValue()), 1.0d);

        double spikeLimit = Math.max(5.0d, baseline * 0.25d);
        return Math.abs(value - previous.getReadingValue()) >= spikeLimit;
    }

    private boolean isContinuousAbnormal(
            IotDevice device,
            MeterType meterType,
            LocalDateTime now,
            AssetThreshold threshold,
            IotAlertSeverity currentSeverity
    ) {
        if (threshold == null || threshold.getWarningThreshold() == null) {
            return false;
        }
        if (!ABNORMAL_SEVERITIES.contains(currentSeverity)) {
            return false;
        }
        long recentAbnormal = iotTelemetryLogRepository.countByDevice_IdAndMeterTypeAndReadingTimeAfterAndSeverityIn(
                device.getId(),
                meterType,
                now.minusMinutes(30),
                ABNORMAL_SEVERITIES
        );
        return recentAbnormal >= 2;
    }

    private RequestPriority mapSeverityToRequestPriority(IotAlertSeverity severity) {
        return switch (severity) {
            case LOW -> RequestPriority.LOW;
            case MEDIUM -> RequestPriority.MEDIUM;
            case HIGH -> RequestPriority.HIGH;
            case CRITICAL -> RequestPriority.CRITICAL;
        };
    }

    private Double resolveThresholdValue(AssetThreshold threshold, IotAlertSeverity severity) {
        if (threshold == null) {
            return null;
        }
        if (severity == IotAlertSeverity.MEDIUM) {
            return threshold.getWarningThreshold();
        }
        return threshold.getCriticalThreshold() != null
                ? threshold.getCriticalThreshold()
                : threshold.getWarningThreshold();
    }

    private String buildAlertMessage(
            IotDevice device,
            MeterType meterType,
            Double readingValue,
            IotAlertSeverity severity,
            IotAlertType alertType
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Device ").append(device.getDeviceUid())
                .append(" reported ").append(meterType)
                .append("=").append(readingValue)
                .append(" [").append(severity).append("]");
        if (alertType != null && alertType != IotAlertType.THRESHOLD_BREACH) {
            sb.append(" due to ").append(alertType);
        }
        return sb.toString();
    }

    private String buildServiceRequestDescription(IotAlert alert, Double readingValue) {
        return "Auto-generated from IoT alert. "
                + "Device=" + alert.getDevice().getDeviceUid()
                + ", Meter=" + alert.getMeterType()
                + ", Reading=" + readingValue
                + ", Severity=" + alert.getSeverity()
                + ", AlertType=" + alert.getAlertType()
                + ", Location=" + alert.getLocation()
                + ", Time=" + alert.getOccurredAt();
    }

    private String buildResultMessage(IotAlertSeverity severity, boolean suddenSpike, boolean continuousAbnormal) {
        if (!ABNORMAL_SEVERITIES.contains(severity)) {
            return "Telemetry recorded";
        }
        if (continuousAbnormal) {
            return "Critical continuous abnormal pattern detected";
        }
        if (suddenSpike) {
            return "Sudden spike detected";
        }
        return "Threshold breach detected";
    }

    private double generateSampleValue(int index, AssetThreshold threshold) {
        double warning = threshold != null && threshold.getWarningThreshold() != null ? threshold.getWarningThreshold() : 70d;
        double critical = threshold != null && threshold.getCriticalThreshold() != null ? threshold.getCriticalThreshold() : 90d;

        int mode = index % 4;
        return switch (mode) {
            case 0 -> warning - ThreadLocalRandom.current().nextDouble(5d, 12d);
            case 1 -> warning + ThreadLocalRandom.current().nextDouble(1d, 6d);
            case 2 -> critical + ThreadLocalRandom.current().nextDouble(1d, 8d);
            default -> critical + ThreadLocalRandom.current().nextDouble(10d, 22d);
        };
    }

    private void logAlertAction(IotAlert alert, String actionType, String actionBy, String details) {
        IotAlertAction action = IotAlertAction.builder()
                .alert(alert)
                .companyId(alert.getCompanyId())
                .actionType(actionType)
                .actionBy(normalize(actionBy))
                .actionDetails(normalize(details))
                .build();
        iotAlertActionRepository.save(action);
        log.info("IoT alert action logged: companyId={}, alertId={}, actionType={}, actionBy={}",
                alert.getCompanyId(), alert.getId(), actionType, normalize(actionBy));
    }

    private IotDevice getDeviceOrThrow(Long id, Long companyId) {
        return iotDeviceRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
    }

    private IotAlert getAlertOrThrow(Long id, Long companyId) {
        return iotAlertRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));
    }

    private Asset resolveAssetForCompany(Long assetId, Long companyId) {
        if (assetId == null) {
            return null;
        }
        return assetRepository.findByIdAndCompanyId(assetId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset not found"));
    }

    private String resolveLocation(Asset asset, String providedLocation) {
        String normalized = normalize(providedLocation);
        if (normalized != null) {
            return normalized;
        }
        if (asset == null) {
            return null;
        }
        return assetLocationRepository.findByAsset_Id(asset.getId())
                .map(loc -> normalize(loc.getLocation()))
                .orElse(null);
    }

    private int normalizeOfflineWindow(Integer offlineAfterMinutes) {
        if (offlineAfterMinutes == null) {
            return 10;
        }
        return Math.min(Math.max(offlineAfterMinutes, 1), 240);
    }

    private IotAlertSeverity maxSeverity(IotAlertSeverity a, IotAlertSeverity b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }

    private Long requireCompanyContext() {
        return CompanyContextHolder.getCompanyId()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyId is required"));
    }

    private String normalizeRequired(String value, String field) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return normalized;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String generateProvisioningToken() {
        byte[] random = new byte[32];
        SECURE_RANDOM.nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    private String resolveDeviceStatus(IotDevice device, int offlineAfterMinutes) {
        if (!Boolean.TRUE.equals(device.getEnabled())) {
            return "DISABLED";
        }
        if (device.getLastSeenAt() == null) {
            return "OFFLINE";
        }
        boolean online = device.getLastSeenAt().isAfter(LocalDateTime.now().minusMinutes(offlineAfterMinutes));
        return online ? "ONLINE" : "OFFLINE";
    }

    private IotDeviceResponse toDeviceResponse(IotDevice device, String provisioningToken, int offlineAfterMinutes) {
        return IotDeviceResponse.builder()
                .id(device.getId())
                .deviceUid(device.getDeviceUid())
                .deviceName(device.getDeviceName())
                .assetId(device.getAsset() != null ? device.getAsset().getId() : null)
                .assetName(device.getAsset() != null ? device.getAsset().getAssetName() : null)
                .location(device.getLocation())
                .enabled(device.getEnabled())
                .lastSeenAt(device.getLastSeenAt())
                .status(resolveDeviceStatus(device, offlineAfterMinutes))
                .createdAt(device.getCreatedAt())
                .updatedAt(device.getUpdatedAt())
                .provisioningToken(provisioningToken)
                .build();
    }

    private IotAlertResponse toAlertResponse(IotAlert alert) {
        return IotAlertResponse.builder()
                .id(alert.getId())
                .deviceUid(alert.getDevice() != null ? alert.getDevice().getDeviceUid() : null)
                .deviceName(alert.getDevice() != null ? alert.getDevice().getDeviceName() : null)
                .assetId(alert.getAsset() != null ? alert.getAsset().getId() : null)
                .assetName(alert.getAsset() != null ? alert.getAsset().getAssetName() : null)
                .location(alert.getLocation())
                .meterType(alert.getMeterType())
                .readingValue(alert.getReadingValue())
                .thresholdValue(alert.getThresholdValue())
                .severity(alert.getSeverity())
                .alertType(alert.getAlertType())
                .status(alert.getStatus())
                .message(alert.getMessage())
                .serviceRequestId(alert.getServiceRequest() != null ? alert.getServiceRequest().getId() : null)
                .serviceRequestCode(alert.getServiceRequest() != null ? alert.getServiceRequest().getRequestId() : null)
                .occurredAt(alert.getOccurredAt())
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .build();
    }

}
