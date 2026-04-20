package com.example.eam.Iot.Service;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Enum.IngestProcessingStatus;
import com.example.eam.Enum.IotAlertSeverity;
import com.example.eam.Enum.IotAlertStatus;
import com.example.eam.Enum.IotAnomalyType;
import com.example.eam.Enum.IotRuleOperator;
import com.example.eam.Iot.Entity.IotAlert;
import com.example.eam.Iot.Entity.IotAlertAction;
import com.example.eam.Iot.Entity.IotAlertRule;
import com.example.eam.Iot.Entity.IotTelemetryLog;
import com.example.eam.Iot.Repository.IotAlertActionRepository;
import com.example.eam.Iot.Repository.IotAlertRepository;
import com.example.eam.Iot.Repository.IotAlertRuleRepository;
import com.example.eam.Iot.Repository.IotTelemetryLogRepository;
import com.example.eam.ServiceMaintenance.Entity.ServiceMaintenance;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class IotAlertEngineService {

    private static final Set<IotAlertStatus> OPEN_STATUSES = EnumSet.of(
            IotAlertStatus.ACTIVE,
            IotAlertStatus.ACKNOWLEDGED,
            IotAlertStatus.SUPPRESSED
    );

    private static final int HEALTHY_STREAK_TARGET = 5;

    private final IotAlertRuleService alertRuleService;
    private final IotAlertRuleRepository alertRuleRepository;
    private final IotAlertRepository alertRepository;
    private final IotAlertActionRepository alertActionRepository;
    private final IotTelemetryLogRepository telemetryLogRepository;
    private final IotServiceRequestFactory serviceRequestFactory;

    @Transactional
    public void processTelemetryLog(IotTelemetryLog telemetry) {
        try {
            if (telemetry.getAsset() == null) {
                markProcessed(telemetry, null, null);
                return;
            }

            IotAlertRule rule = alertRuleService.findActiveRule(
                    telemetry.getCompanyId(),
                    telemetry.getAsset().getId(),
                    telemetry.getMetricCode()
            );
            if (rule == null) {
                markProcessed(telemetry, null, null);
                return;
            }

            Evaluation evaluation = evaluate(telemetry, rule);
            if (evaluation.abnormal()) {
                handleAbnormalReading(telemetry, rule, evaluation);
                markProcessed(telemetry, evaluation.severity(), evaluation.anomalyType());
            } else {
                handleHealthyReading(telemetry);
                markProcessed(telemetry, null, null);
            }
        } catch (Exception ex) {
            telemetry.setIngestStatus(IngestProcessingStatus.FAILED);
            telemetry.setProcessingAttempts(safeInt(telemetry.getProcessingAttempts()) + 1);
            telemetry.setLastError(ex.getMessage());
            telemetryLogRepository.save(telemetry);
            throw ex;
        }
    }

    private void handleAbnormalReading(IotTelemetryLog telemetry, IotAlertRule rule, Evaluation evaluation) {
        LocalDateTime now = telemetry.getObservedAt() != null ? telemetry.getObservedAt() : LocalDateTime.now();
        List<IotAlert> openAlerts = alertRepository.findOpenAlertsForMetric(
                telemetry.getCompanyId(),
                telemetry.getAsset().getId(),
                telemetry.getMetricCode(),
                OPEN_STATUSES,
                PageRequest.of(0, 1)
        );
        IotAlert existing = openAlerts.isEmpty() ? null : openAlerts.getFirst();

        if (existing == null && isCooldownActive(rule, now)) {
            return;
        }

        IotAlertSeverity previousSeverity = existing != null ? existing.getSeverity() : null;
        IotAlert target = existing != null ? existing : IotAlert.builder()
                .companyId(telemetry.getCompanyId())
                .device(telemetry.getDevice())
                .asset(telemetry.getAsset())
                .rule(rule)
                .metric(telemetry.getMetric())
                .metricCode(telemetry.getMetricCode())
                .occurredAt(now)
                .healthyStreak(0)
                .build();

        target.setStatus(IotAlertStatus.ACTIVE);
        target.setLatestValue(telemetry.getReadingValue());
        target.setThresholdValue(evaluation.thresholdValue());
        target.setSeverity(evaluation.severity());
        target.setAnomalyType(evaluation.anomalyType());
        target.setLocation(resolveLocation(telemetry, rule));
        target.setMessage(buildMessage(telemetry, evaluation));
        target.setLastTriggeredAt(now);
        target.setLastNormalAt(null);
        target.setHealthyStreak(0);

        IotAlert saved = alertRepository.save(target);
        rule.setLastTriggeredAt(now);
        alertRuleRepository.save(rule);

        writeAction(saved,
                existing == null ? "ALERT_RAISED" : "ALERT_UPDATED",
                saved.getMessage());

        boolean shouldCreateServiceRequest = rule.isAutoCreateServiceRequest()
                && saved.getLinkedServiceRequest() == null
                && (existing == null || rank(saved.getSeverity()) > rank(previousSeverity));

        if (shouldCreateServiceRequest) {
            ServiceMaintenance sr = serviceRequestFactory.createForAlert(
                    telemetry.getCompanyId(),
                    telemetry.getAsset(),
                    saved.getLocation(),
                    saved.getSeverity(),
                    "IoT alert: " + telemetry.getMetricCode(),
                    saved.getMessage(),
                    telemetry.getDevice() != null ? telemetry.getDevice().getDeviceUid() : "IOT-SYSTEM"
            );
            saved.setLinkedServiceRequest(sr);
            alertRepository.save(saved);
            writeAction(saved, "SERVICE_REQUEST_CREATED", "Service request " + sr.getRequestId() + " linked");
        }
    }

    private void handleHealthyReading(IotTelemetryLog telemetry) {
        if (telemetry.getAsset() == null) {
            return;
        }
        List<IotAlert> openAlerts = alertRepository.findOpenAlertsForMetric(
                telemetry.getCompanyId(),
                telemetry.getAsset().getId(),
                telemetry.getMetricCode(),
                OPEN_STATUSES,
                PageRequest.of(0, 1)
        );
        if (openAlerts.isEmpty()) {
            return;
        }
        IotAlert alert = openAlerts.getFirst();
        int nextStreak = safeInt(alert.getHealthyStreak()) + 1;
        alert.setHealthyStreak(nextStreak);
        alert.setLastNormalAt(telemetry.getObservedAt());
        if (nextStreak >= HEALTHY_STREAK_TARGET) {
            alert.setStatus(IotAlertStatus.AUTO_RESOLVED);
            alert.setResolvedBy("SYSTEM");
            alert.setResolvedAt(LocalDateTime.now());
            writeAction(alert, "AUTO_RESOLVED", "Auto-resolved after healthy streak");
        }
        alertRepository.save(alert);
    }

    private Evaluation evaluate(IotTelemetryLog telemetry, IotAlertRule rule) {
        double value = telemetry.getReadingValue();
        IotAlertSeverity severity = resolveSeverity(rule, value);
        IotAnomalyType anomalyType = severity != null ? IotAnomalyType.THRESHOLD_BREACH : null;
        Double thresholdValue = resolveThresholdValue(rule, severity);

        List<IotTelemetryLog> recent = telemetryLogRepository
                .findByCompanyIdAndAsset_IdAndMetricCodeOrderByObservedAtDesc(
                        telemetry.getCompanyId(),
                        telemetry.getAsset().getId(),
                        telemetry.getMetricCode(),
                        PageRequest.of(0, 10)
                );

        IotTelemetryLog previous = recent.stream()
                .filter(row -> !row.getId().equals(telemetry.getId()))
                .findFirst()
                .orElse(null);

        if (rule.getSpikeDelta() != null && previous != null) {
            double delta = Math.abs(value - previous.getReadingValue());
            if (delta >= rule.getSpikeDelta()) {
                severity = maxSeverity(severity, IotAlertSeverity.HIGH);
                anomalyType = IotAnomalyType.SUDDEN_SPIKE;
                if (thresholdValue == null) {
                    thresholdValue = previous.getReadingValue();
                }
            }
        }

        if (rule.getConsecutiveAbnormalCount() != null && rule.getConsecutiveAbnormalCount() > 1) {
            long recentAbnormal = recent.stream()
                    .filter(row -> row.getAlertSeverity() != null)
                    .count();
            if (recentAbnormal + (severity != null ? 1 : 0) >= rule.getConsecutiveAbnormalCount()) {
                severity = maxSeverity(severity, IotAlertSeverity.HIGH);
                anomalyType = IotAnomalyType.CONSECUTIVE_ABNORMAL;
            }
        }

        return new Evaluation(severity != null, severity, anomalyType, thresholdValue);
    }

    private IotAlertSeverity resolveSeverity(IotAlertRule rule, double value) {
        IotAlertSeverity severity = null;
        if (isBreached(rule.getLowThreshold(), value, rule.getRuleOperator())) {
            severity = IotAlertSeverity.LOW;
        }
        if (isBreached(rule.getMediumThreshold(), value, rule.getRuleOperator())) {
            severity = IotAlertSeverity.MEDIUM;
        }
        if (isBreached(rule.getHighThreshold(), value, rule.getRuleOperator())) {
            severity = IotAlertSeverity.HIGH;
        }
        if (isBreached(rule.getCriticalThreshold(), value, rule.getRuleOperator())) {
            severity = IotAlertSeverity.CRITICAL;
        }
        return severity;
    }

    private Double resolveThresholdValue(IotAlertRule rule, IotAlertSeverity severity) {
        if (severity == null) {
            return null;
        }
        return switch (severity) {
            case LOW -> rule.getLowThreshold();
            case MEDIUM -> rule.getMediumThreshold();
            case HIGH -> rule.getHighThreshold();
            case CRITICAL -> rule.getCriticalThreshold();
        };
    }

    private boolean isBreached(Double threshold, double value, IotRuleOperator operator) {
        if (threshold == null) {
            return false;
        }
        return switch (operator) {
            case ABOVE -> value >= threshold;
            case BELOW -> value <= threshold;
        };
    }

    private boolean isCooldownActive(IotAlertRule rule, LocalDateTime now) {
        if (rule.getLastTriggeredAt() == null || rule.getCooldownMinutes() == null || rule.getCooldownMinutes() <= 0) {
            return false;
        }
        return now.isBefore(rule.getLastTriggeredAt().plusMinutes(rule.getCooldownMinutes()));
    }

    private void markProcessed(IotTelemetryLog telemetry, IotAlertSeverity severity, IotAnomalyType anomalyType) {
        telemetry.setIngestStatus(IngestProcessingStatus.PROCESSED);
        telemetry.setAlertSeverity(severity);
        telemetry.setAnomalyType(anomalyType);
        telemetry.setProcessedAt(LocalDateTime.now());
        telemetry.setProcessingAttempts(safeInt(telemetry.getProcessingAttempts()) + 1);
        telemetry.setLastError(null);
        telemetryLogRepository.save(telemetry);
    }

    private void writeAction(IotAlert alert, String actionType, String details) {
        alertActionRepository.save(IotAlertAction.builder()
                .alert(alert)
                .companyId(alert.getCompanyId())
                .actionType(actionType)
                .actionBy("SYSTEM")
                .actionDetails(details)
                .build());
    }

    private String buildMessage(IotTelemetryLog telemetry, Evaluation evaluation) {
        Asset asset = telemetry.getAsset();
        String assetName = asset != null ? asset.getAssetName() : "Unknown asset";
        return "Metric " + telemetry.getMetricCode()
                + " reported " + telemetry.getReadingValue()
                + " for " + assetName
                + " (" + evaluation.anomalyType() + ", " + evaluation.severity() + ")";
    }

    private String resolveLocation(IotTelemetryLog telemetry, IotAlertRule rule) {
        if (telemetry.getLocation() != null && !telemetry.getLocation().isBlank()) {
            return telemetry.getLocation().trim();
        }
        if (rule.getLocation() != null && !rule.getLocation().isBlank()) {
            return rule.getLocation().trim();
        }
        if (telemetry.getDevice() != null && telemetry.getDevice().getLocation() != null) {
            return telemetry.getDevice().getLocation();
        }
        if (telemetry.getAsset() != null && telemetry.getAsset().getLocation() != null) {
            return telemetry.getAsset().getLocation().getLocation();
        }
        return null;
    }

    private IotAlertSeverity maxSeverity(IotAlertSeverity left, IotAlertSeverity right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return rank(left) >= rank(right) ? left : right;
    }

    private int rank(IotAlertSeverity severity) {
        if (severity == null) {
            return 0;
        }
        return switch (severity) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
            case CRITICAL -> 4;
        };
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private record Evaluation(boolean abnormal,
                              IotAlertSeverity severity,
                              IotAnomalyType anomalyType,
                              Double thresholdValue) {
    }
}

