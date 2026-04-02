package com.example.eam.Iot.Service;

import com.example.eam.Common.CompanyContextHolder;
import com.example.eam.Enum.IotAlertSeverity;
import com.example.eam.Enum.IotAlertStatus;
import com.example.eam.Iot.Dto.IotAlertActionRequest;
import com.example.eam.Iot.Dto.IotAlertResponse;
import com.example.eam.Iot.Entity.IotAlert;
import com.example.eam.Iot.Entity.IotAlertAction;
import com.example.eam.Iot.Repository.IotAlertActionRepository;
import com.example.eam.Iot.Repository.IotAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class IotAlertService {

    private static final Set<IotAlertStatus> OPEN_STATUSES = EnumSet.of(
            IotAlertStatus.ACTIVE,
            IotAlertStatus.ACKNOWLEDGED,
            IotAlertStatus.SUPPRESSED
    );

    private static final int DEFAULT_AUTO_RESOLVE_HEALTHY_STREAK = 5;

    private final IotAlertRepository alertRepository;
    private final IotAlertActionRepository alertActionRepository;

    @Transactional(readOnly = true)
    public Page<IotAlertResponse> list(Long assetId,
                                       String location,
                                       IotAlertSeverity severity,
                                       IotAlertStatus status,
                                       LocalDateTime from,
                                       LocalDateTime to,
                                       Pageable pageable) {
        Long companyId = requireCompanyId();
        return alertRepository.search(companyId, assetId, normalizeOptional(location), severity, status, from, to, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public IotAlertResponse acknowledge(Long id, IotAlertActionRequest request) {
        IotAlert alert = getOrThrow(id, requireCompanyId());
        alert.setStatus(IotAlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedBy(currentUserOrSystem());
        alert.setAcknowledgedAt(LocalDateTime.now());
        IotAlert saved = alertRepository.save(alert);
        writeAction(saved, "ACKNOWLEDGED", request != null ? request.getReason() : null);
        return toResponse(saved);
    }

    @Transactional
    public IotAlertResponse suppress(Long id, IotAlertActionRequest request) {
        IotAlert alert = getOrThrow(id, requireCompanyId());
        alert.setStatus(IotAlertStatus.SUPPRESSED);
        alert.setSuppressedUntil(request != null ? request.getSuppressedUntil() : null);
        IotAlert saved = alertRepository.save(alert);
        writeAction(saved, "SUPPRESSED", request != null ? request.getReason() : null);
        return toResponse(saved);
    }

    @Transactional
    public IotAlertResponse resolve(Long id, IotAlertActionRequest request) {
        IotAlert alert = getOrThrow(id, requireCompanyId());
        alert.setStatus(IotAlertStatus.RESOLVED);
        alert.setResolvedBy(currentUserOrSystem());
        alert.setResolvedAt(LocalDateTime.now());
        IotAlert saved = alertRepository.save(alert);
        writeAction(saved, "RESOLVED", request != null ? request.getReason() : null);
        return toResponse(saved);
    }

    @Transactional
    public int autoResolveEligibleAlerts() {
        java.util.List<IotAlert> candidates = alertRepository
                .findByStatusInAndLastNormalAtIsNotNullAndHealthyStreakGreaterThanEqual(OPEN_STATUSES, DEFAULT_AUTO_RESOLVE_HEALTHY_STREAK);
        int resolved = 0;
        for (IotAlert alert : candidates) {
            alert.setStatus(IotAlertStatus.AUTO_RESOLVED);
            alert.setResolvedBy("SYSTEM");
            alert.setResolvedAt(LocalDateTime.now());
            alertRepository.save(alert);
            writeAction(alert, "AUTO_RESOLVED", "Automatically resolved after healthy streak");
            resolved++;
        }
        return resolved;
    }

    @Transactional(readOnly = true)
    public long countActiveAlerts(Long companyId) {
        return alertRepository.countByCompanyIdAndStatusIn(companyId, OPEN_STATUSES);
    }

    @Transactional(readOnly = true)
    public long countCriticalActiveAlerts(Long companyId) {
        return alertRepository.search(
                        companyId,
                        null,
                        null,
                        IotAlertSeverity.CRITICAL,
                        null,
                        null,
                        null,
                        Pageable.unpaged())
                .stream()
                .filter(a -> OPEN_STATUSES.contains(a.getStatus()))
                .count();
    }

    public IotAlertResponse toResponse(IotAlert alert) {
        return IotAlertResponse.builder()
                .id(alert.getId())
                .deviceId(alert.getDevice() != null ? alert.getDevice().getId() : null)
                .deviceUid(alert.getDevice() != null ? alert.getDevice().getDeviceUid() : null)
                .assetId(alert.getAsset() != null ? alert.getAsset().getId() : null)
                .assetName(alert.getAsset() != null ? alert.getAsset().getAssetName() : null)
                .ruleId(alert.getRule() != null ? alert.getRule().getId() : null)
                .metricId(alert.getMetric() != null ? alert.getMetric().getId() : null)
                .metricCode(alert.getMetricCode())
                .latestValue(alert.getLatestValue())
                .thresholdValue(alert.getThresholdValue())
                .severity(alert.getSeverity())
                .anomalyType(alert.getAnomalyType())
                .status(alert.getStatus())
                .location(alert.getLocation())
                .message(alert.getMessage())
                .linkedServiceRequestDbId(alert.getLinkedServiceRequest() != null ? alert.getLinkedServiceRequest().getId() : null)
                .linkedServiceRequestId(alert.getLinkedServiceRequest() != null ? alert.getLinkedServiceRequest().getRequestId() : null)
                .occurredAt(alert.getOccurredAt())
                .lastTriggeredAt(alert.getLastTriggeredAt())
                .lastNormalAt(alert.getLastNormalAt())
                .healthyStreak(alert.getHealthyStreak())
                .acknowledgedBy(alert.getAcknowledgedBy())
                .acknowledgedAt(alert.getAcknowledgedAt())
                .resolvedBy(alert.getResolvedBy())
                .resolvedAt(alert.getResolvedAt())
                .suppressedUntil(alert.getSuppressedUntil())
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .build();
    }

    private IotAlert getOrThrow(Long id, Long companyId) {
        return alertRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "IoT alert not found"));
    }

    private Long requireCompanyId() {
        return CompanyContextHolder.getCompanyId()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyId query parameter is required"));
    }

    private void writeAction(IotAlert alert, String actionType, String details) {
        alertActionRepository.save(IotAlertAction.builder()
                .alert(alert)
                .companyId(alert.getCompanyId())
                .actionType(actionType)
                .actionBy(currentUserOrSystem())
                .actionDetails(normalizeOptional(details))
                .build());
    }

    private String currentUserOrSystem() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return "SYSTEM";
        }
        String principal = String.valueOf(auth.getPrincipal()).trim();
        return principal.isEmpty() ? "SYSTEM" : principal;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

