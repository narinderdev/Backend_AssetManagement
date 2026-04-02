package com.example.eam.Iot.Service;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Enum.IngestProcessingStatus;
import com.example.eam.Enum.IotRuleOperator;
import com.example.eam.Iot.Entity.IotAlertRule;
import com.example.eam.Iot.Entity.IotMetricCatalog;
import com.example.eam.Iot.Entity.IotTelemetryLog;
import com.example.eam.Iot.Repository.IotAlertRuleRepository;
import com.example.eam.Iot.Repository.IotMetricCatalogRepository;
import com.example.eam.Iot.Repository.IotTelemetryLogRepository;
import com.example.eam.Maintenance.Predictive.Entity.AssetThreshold;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IotPredictiveBridgeService {

    private final IotMetricCatalogRepository metricRepository;
    private final IotAlertRuleRepository ruleRepository;
    private final IotTelemetryLogRepository telemetryLogRepository;
    private final IotAlertEngineService alertEngineService;

    @Transactional
    public void processPredictiveReading(Asset asset,
                                         AssetThreshold threshold,
                                         String metricCode,
                                         Double value,
                                         LocalDateTime readingTime,
                                         String notes) {
        if (asset == null || asset.getCompanyId() == null || metricCode == null || value == null) {
            return;
        }

        Long companyId = asset.getCompanyId();
        String normalizedMetricCode = metricCode.trim().toUpperCase();

        IotMetricCatalog metric = metricRepository.findByCompanyIdAndMetricCode(companyId, normalizedMetricCode)
                .orElseGet(() -> metricRepository.save(IotMetricCatalog.builder()
                        .companyId(companyId)
                        .metricCode(normalizedMetricCode)
                        .metricName(normalizedMetricCode.replace('_', ' '))
                        .active(true)
                        .build()));

        IotAlertRule rule = ruleRepository.findByCompanyIdAndAsset_IdAndMetric_Id(companyId, asset.getId(), metric.getId())
                .orElseGet(() -> IotAlertRule.builder()
                        .companyId(companyId)
                        .asset(asset)
                        .metric(metric)
                        .ruleOperator(IotRuleOperator.ABOVE)
                        .active(true)
                        .autoCreateServiceRequest(threshold != null && Boolean.TRUE.equals(threshold.getAutoCreateWo()))
                        .cooldownMinutes(threshold != null && threshold.getCooldownHours() != null
                                ? Math.max(1, threshold.getCooldownHours() * 60)
                                : 60)
                        .build());

        if (threshold != null) {
            rule.setLocation(threshold.getLocation());
            rule.setLowThreshold(threshold.getWarningThreshold());
            rule.setMediumThreshold(threshold.getWarningThreshold());
            rule.setHighThreshold(threshold.getCriticalThreshold());
            rule.setCriticalThreshold(threshold.getCriticalThreshold());
            rule.setAutoCreateServiceRequest(Boolean.TRUE.equals(threshold.getAutoCreateWo()));
            rule.setCooldownMinutes(threshold.getCooldownHours() != null ? Math.max(1, threshold.getCooldownHours() * 60) : 60);
            rule.setActive(true);
        }
        rule = ruleRepository.save(rule);

        IotTelemetryLog log = telemetryLogRepository.save(IotTelemetryLog.builder()
                .companyId(companyId)
                .device(null)
                .asset(asset)
                .metric(metric)
                .metricCode(normalizedMetricCode)
                .eventId(null)
                .observedAt(readingTime != null ? readingTime : LocalDateTime.now())
                .readingValue(value)
                .location(threshold != null ? threshold.getLocation() : null)
                .ingestStatus(IngestProcessingStatus.PENDING)
                .processingAttempts(0)
                .lastError(notes)
                .build());

        alertEngineService.processTelemetryLog(log);
    }
}

