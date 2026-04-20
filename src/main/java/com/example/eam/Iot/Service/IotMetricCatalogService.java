package com.example.eam.Iot.Service;

import com.example.eam.Common.CompanyContextHolder;
import com.example.eam.Iot.Dto.IotMetricCreateRequest;
import com.example.eam.Iot.Dto.IotMetricPatchRequest;
import com.example.eam.Iot.Dto.IotMetricResponse;
import com.example.eam.Iot.Entity.IotMetricCatalog;
import com.example.eam.Iot.Repository.IotMetricCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class IotMetricCatalogService {

    private final IotMetricCatalogRepository metricRepository;

    @Transactional
    public IotMetricResponse create(IotMetricCreateRequest request) {
        Long companyId = requireCompanyId();
        String metricCode = normalizeRequired(request.getMetricCode(), "metricCode").toUpperCase();
        if (metricRepository.existsByCompanyIdAndMetricCode(companyId, metricCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Metric code already exists");
        }
        IotMetricCatalog saved = metricRepository.save(IotMetricCatalog.builder()
                .companyId(companyId)
                .metricCode(metricCode)
                .metricName(normalizeRequired(request.getMetricName(), "metricName"))
                .unit(normalizeOptional(request.getUnit()))
                .description(normalizeOptional(request.getDescription()))
                .active(request.getActive() == null || request.getActive())
                .build());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public IotMetricResponse get(Long id) {
        return toResponse(getOrThrow(id, requireCompanyId()));
    }

    @Transactional(readOnly = true)
    public Page<IotMetricResponse> list(Pageable pageable) {
        Long companyId = requireCompanyId();
        return metricRepository.findByCompanyId(companyId, pageable).map(this::toResponse);
    }

    @Transactional
    public IotMetricResponse patch(Long id, IotMetricPatchRequest request) {
        Long companyId = requireCompanyId();
        IotMetricCatalog metric = getOrThrow(id, companyId);

        if (request.getMetricCode() != null && !request.getMetricCode().isBlank()) {
            String newCode = request.getMetricCode().trim().toUpperCase();
            if (metricRepository.existsByCompanyIdAndMetricCodeAndIdNot(companyId, newCode, metric.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Metric code already exists");
            }
            metric.setMetricCode(newCode);
        }
        if (request.getMetricName() != null && !request.getMetricName().isBlank()) {
            metric.setMetricName(request.getMetricName().trim());
        }
        if (request.getUnit() != null) {
            metric.setUnit(normalizeOptional(request.getUnit()));
        }
        if (request.getDescription() != null) {
            metric.setDescription(normalizeOptional(request.getDescription()));
        }
        if (request.getActive() != null) {
            metric.setActive(request.getActive());
        }

        return toResponse(metricRepository.save(metric));
    }

    public IotMetricCatalog getByCodeOrThrow(Long companyId, String metricCode) {
        IotMetricCatalog metric = metricRepository.findByCompanyIdAndMetricCode(companyId, metricCode.toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Metric not found: " + metricCode));
        if (!metric.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Metric is inactive: " + metricCode);
        }
        return metric;
    }

    private IotMetricCatalog getOrThrow(Long id, Long companyId) {
        return metricRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "IoT metric not found"));
    }

    private Long requireCompanyId() {
        return CompanyContextHolder.getCompanyId()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyId query parameter is required"));
    }

    private String normalizeRequired(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public IotMetricResponse toResponse(IotMetricCatalog entity) {
        return IotMetricResponse.builder()
                .id(entity.getId())
                .metricCode(entity.getMetricCode())
                .metricName(entity.getMetricName())
                .unit(entity.getUnit())
                .description(entity.getDescription())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
