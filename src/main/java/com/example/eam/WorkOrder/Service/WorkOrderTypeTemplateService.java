package com.example.eam.WorkOrder.Service;

import com.example.eam.WorkOrder.Dto.WorkOrderTypeTemplateCreateRequest;
import com.example.eam.WorkOrder.Dto.WorkOrderTypeTemplateResponse;
import com.example.eam.WorkOrder.Entity.WorkOrderTypeTemplate;
import com.example.eam.WorkOrder.Repository.WorkOrderTypeTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class WorkOrderTypeTemplateService {

    private final WorkOrderTypeTemplateRepository repository;

    @Transactional
    public WorkOrderTypeTemplateResponse create(WorkOrderTypeTemplateCreateRequest req) {
        String type = req.getWorkOrderType().trim();
        if (repository.existsByWorkOrderTypeIgnoreCase(type)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Work order type already exists: " + type);
        }
        WorkOrderTypeTemplate saved = repository.save(WorkOrderTypeTemplate.builder()
                .workOrderType(type)
                .defaultGlAccount(trim(req.getDefaultGlAccount()))
                .defaultUtilityAccount(trim(req.getDefaultUtilityAccount()))
                .costTreatment(req.getCostTreatment())
                .laborGlAccount(trim(req.getLaborGlAccount()))
                .laborUtilityAccount(trim(req.getLaborUtilityAccount()))
                .inventoryGlAccount(trim(req.getInventoryGlAccount()))
                .inventoryUtilityAccount(trim(req.getInventoryUtilityAccount()))
                .active(req.getActive() == null || req.getActive())
                .build());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public WorkOrderTypeTemplateResponse get(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<WorkOrderTypeTemplateResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    private WorkOrderTypeTemplate getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work order type not found"));
    }

    private WorkOrderTypeTemplateResponse toResponse(WorkOrderTypeTemplate t) {
        return WorkOrderTypeTemplateResponse.builder()
                .id(t.getId())
                .workOrderType(t.getWorkOrderType())
                .defaultGlAccount(t.getDefaultGlAccount())
                .defaultUtilityAccount(t.getDefaultUtilityAccount())
                .costTreatment(t.getCostTreatment())
                .laborGlAccount(t.getLaborGlAccount())
                .laborUtilityAccount(t.getLaborUtilityAccount())
                .inventoryGlAccount(t.getInventoryGlAccount())
                .inventoryUtilityAccount(t.getInventoryUtilityAccount())
                .active(t.isActive())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private String trim(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
