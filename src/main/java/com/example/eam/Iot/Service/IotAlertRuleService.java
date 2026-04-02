package com.example.eam.Iot.Service;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Asset.Repository.AssetRepository;
import com.example.eam.Common.CompanyContextHolder;
import com.example.eam.Iot.Dto.IotRuleCreateRequest;
import com.example.eam.Iot.Dto.IotRulePatchRequest;
import com.example.eam.Iot.Dto.IotRuleResponse;
import com.example.eam.Iot.Entity.IotAlertRule;
import com.example.eam.Iot.Entity.IotMetricCatalog;
import com.example.eam.Iot.Repository.IotAlertRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class IotAlertRuleService {

    private final IotAlertRuleRepository ruleRepository;
    private final AssetRepository assetRepository;
    private final IotMetricCatalogService metricCatalogService;

    @Transactional
    public IotRuleResponse create(IotRuleCreateRequest request) {
        Long companyId = requireCompanyId();
        Asset asset = resolveAsset(request.getAssetId(), companyId);
        IotMetricCatalog metric = metricCatalogService.getByCodeOrThrow(companyId, request.getMetricCode());

        ruleRepository.findByCompanyIdAndAsset_IdAndMetric_Id(companyId, asset.getId(), metric.getId())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Rule already exists for asset and metric");
                });

        validateThresholds(request.getLowThreshold(), request.getMediumThreshold(), request.getHighThreshold(), request.getCriticalThreshold());

        IotAlertRule saved = ruleRepository.save(IotAlertRule.builder()
                .companyId(companyId)
                .asset(asset)
                .metric(metric)
                .location(normalizeOptional(request.getLocation()))
                .ruleOperator(request.getRuleOperator())
                .lowThreshold(request.getLowThreshold())
                .mediumThreshold(request.getMediumThreshold())
                .highThreshold(request.getHighThreshold())
                .criticalThreshold(request.getCriticalThreshold())
                .cooldownMinutes(request.getCooldownMinutes() != null ? request.getCooldownMinutes() : 60)
                .spikeDelta(request.getSpikeDelta())
                .consecutiveAbnormalCount(request.getConsecutiveAbnormalCount())
                .autoCreateServiceRequest(request.getAutoCreateServiceRequest() == null || request.getAutoCreateServiceRequest())
                .active(request.getActive() == null || request.getActive())
                .build());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public IotRuleResponse get(Long id) {
        return toResponse(getOrThrow(id, requireCompanyId()));
    }

    @Transactional(readOnly = true)
    public Page<IotRuleResponse> list(Pageable pageable) {
        Long companyId = requireCompanyId();
        return ruleRepository.findByCompanyId(companyId, pageable).map(this::toResponse);
    }

    @Transactional
    public IotRuleResponse patch(Long id, IotRulePatchRequest request) {
        Long companyId = requireCompanyId();
        IotAlertRule rule = getOrThrow(id, companyId);

        Asset asset = rule.getAsset();
        if (request.getAssetId() != null) {
            asset = resolveAsset(request.getAssetId(), companyId);
            rule.setAsset(asset);
        }

        IotMetricCatalog metric = rule.getMetric();
        if (request.getMetricCode() != null && !request.getMetricCode().isBlank()) {
            metric = metricCatalogService.getByCodeOrThrow(companyId, request.getMetricCode());
            rule.setMetric(metric);
        }

        ruleRepository.findByCompanyIdAndAsset_IdAndMetric_Id(companyId, asset.getId(), metric.getId())
                .filter(existing -> !existing.getId().equals(rule.getId()))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Rule already exists for asset and metric");
                });

        if (request.getLocation() != null) {
            rule.setLocation(normalizeOptional(request.getLocation()));
        }
        if (request.getRuleOperator() != null) {
            rule.setRuleOperator(request.getRuleOperator());
        }
        if (request.getLowThreshold() != null) {
            rule.setLowThreshold(request.getLowThreshold());
        }
        if (request.getMediumThreshold() != null) {
            rule.setMediumThreshold(request.getMediumThreshold());
        }
        if (request.getHighThreshold() != null) {
            rule.setHighThreshold(request.getHighThreshold());
        }
        if (request.getCriticalThreshold() != null) {
            rule.setCriticalThreshold(request.getCriticalThreshold());
        }
        if (request.getCooldownMinutes() != null) {
            rule.setCooldownMinutes(request.getCooldownMinutes());
        }
        if (request.getSpikeDelta() != null) {
            rule.setSpikeDelta(request.getSpikeDelta());
        }
        if (request.getConsecutiveAbnormalCount() != null) {
            rule.setConsecutiveAbnormalCount(request.getConsecutiveAbnormalCount());
        }
        if (request.getAutoCreateServiceRequest() != null) {
            rule.setAutoCreateServiceRequest(request.getAutoCreateServiceRequest());
        }
        if (request.getActive() != null) {
            rule.setActive(request.getActive());
        }

        validateThresholds(rule.getLowThreshold(), rule.getMediumThreshold(), rule.getHighThreshold(), rule.getCriticalThreshold());
        return toResponse(ruleRepository.save(rule));
    }

    @Transactional
    public void delete(Long id) {
        Long companyId = requireCompanyId();
        IotAlertRule rule = getOrThrow(id, companyId);
        ruleRepository.delete(rule);
    }

    public IotAlertRule findActiveRule(Long companyId, Long assetId, String metricCode) {
        return ruleRepository.findByCompanyIdAndAsset_IdAndMetric_MetricCode(companyId, assetId, metricCode.toUpperCase())
                .filter(IotAlertRule::isActive)
                .orElse(null);
    }

    private IotAlertRule getOrThrow(Long id, Long companyId) {
        return ruleRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "IoT alert rule not found"));
    }

    private Asset resolveAsset(Long id, Long companyId) {
        return assetRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset not found"));
    }

    private Long requireCompanyId() {
        return CompanyContextHolder.getCompanyId()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyId query parameter is required"));
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateThresholds(Double low, Double medium, Double high, Double critical) {
        if (low == null && medium == null && high == null && critical == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one threshold must be configured");
        }
    }

    public IotRuleResponse toResponse(IotAlertRule rule) {
        return IotRuleResponse.builder()
                .id(rule.getId())
                .assetId(rule.getAsset() != null ? rule.getAsset().getId() : null)
                .assetName(rule.getAsset() != null ? rule.getAsset().getAssetName() : null)
                .metricId(rule.getMetric() != null ? rule.getMetric().getId() : null)
                .metricCode(rule.getMetric() != null ? rule.getMetric().getMetricCode() : null)
                .metricName(rule.getMetric() != null ? rule.getMetric().getMetricName() : null)
                .location(rule.getLocation())
                .ruleOperator(rule.getRuleOperator())
                .lowThreshold(rule.getLowThreshold())
                .mediumThreshold(rule.getMediumThreshold())
                .highThreshold(rule.getHighThreshold())
                .criticalThreshold(rule.getCriticalThreshold())
                .cooldownMinutes(rule.getCooldownMinutes())
                .spikeDelta(rule.getSpikeDelta())
                .consecutiveAbnormalCount(rule.getConsecutiveAbnormalCount())
                .autoCreateServiceRequest(rule.isAutoCreateServiceRequest())
                .active(rule.isActive())
                .lastTriggeredAt(rule.getLastTriggeredAt())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}

