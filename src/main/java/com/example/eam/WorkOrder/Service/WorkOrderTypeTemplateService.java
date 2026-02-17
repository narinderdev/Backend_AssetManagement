package com.example.eam.WorkOrder.Service;

import com.example.eam.WorkOrder.Dto.WorkOrderTypeTemplateCreateRequest;
import com.example.eam.WorkOrder.Dto.WorkOrderTypeTemplateResponse;
import com.example.eam.WorkOrder.Dto.WorkOrderTypeTemplateUpdateRequest;
import com.example.eam.WorkOrder.Entity.WorkOrderTypeTemplate;
import com.example.eam.WorkOrder.Repository.WorkOrderRepository;
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
    private final WorkOrderRepository workOrderRepository;

    @Transactional
    public WorkOrderTypeTemplateResponse create(WorkOrderTypeTemplateCreateRequest req) {
        String type = req.getWorkOrderType().trim();
        if (repository.existsByWorkOrderTypeIgnoreCase(type)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Work order type already exists: " + type);
        }
        boolean createAsset = Boolean.TRUE.equals(req.getCreateAsset());
        WorkOrderTypeTemplate saved = repository.save(WorkOrderTypeTemplate.builder()
                .workOrderType(type)
                .defaultGlAccount(trim(req.getDefaultGlAccount()))
                .defaultUtilityAccount(trim(req.getDefaultUtilityAccount()))
                .costTreatment(req.getCostTreatment())
                .laborGlAccount(trim(req.getLaborGlAccount()))
                .laborUtilityAccount(trim(req.getLaborUtilityAccount()))
                .inventoryGlAccount(trim(req.getInventoryGlAccount()))
                .inventoryUtilityAccount(trim(req.getInventoryUtilityAccount()))
                .createAsset(createAsset)
                .propertyUnit(createAsset ? trim(req.getPropertyUnit()) : null)
                .propertyGroup(createAsset ? trim(req.getPropertyGroup()) : null)
                .retirementUnit(createAsset ? trim(req.getRetirementUnit()) : null)
                .functionalClass(createAsset ? trim(req.getFunctionalClass()) : null)
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

    @Transactional
    public WorkOrderTypeTemplateResponse update(Long id, WorkOrderTypeTemplateUpdateRequest req) {
        WorkOrderTypeTemplate t = getOrThrow(id);

        if (req.getWorkOrderType() != null) {
            String type = req.getWorkOrderType().trim();
            if (type.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "workOrderType cannot be blank");
            }
            if (repository.existsByWorkOrderTypeIgnoreCaseAndIdNot(type, id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Work order type already exists: " + type);
            }
            t.setWorkOrderType(type);
        }

        if (req.getDefaultGlAccount() != null) t.setDefaultGlAccount(trim(req.getDefaultGlAccount()));
        if (req.getDefaultUtilityAccount() != null) t.setDefaultUtilityAccount(trim(req.getDefaultUtilityAccount()));
        if (req.getCostTreatment() != null) t.setCostTreatment(req.getCostTreatment());
        if (req.getLaborGlAccount() != null) t.setLaborGlAccount(trim(req.getLaborGlAccount()));
        if (req.getLaborUtilityAccount() != null) t.setLaborUtilityAccount(trim(req.getLaborUtilityAccount()));
        if (req.getInventoryGlAccount() != null) t.setInventoryGlAccount(trim(req.getInventoryGlAccount()));
        if (req.getInventoryUtilityAccount() != null) t.setInventoryUtilityAccount(trim(req.getInventoryUtilityAccount()));
        if (req.getCreateAsset() != null) {
            t.setCreateAsset(req.getCreateAsset());
            if (!req.getCreateAsset()) {
                t.setPropertyUnit(null);
                t.setPropertyGroup(null);
                t.setRetirementUnit(null);
                t.setFunctionalClass(null);
            }
        }
        if (req.getPropertyUnit() != null) t.setPropertyUnit(trim(req.getPropertyUnit()));
        if (req.getPropertyGroup() != null) t.setPropertyGroup(trim(req.getPropertyGroup()));
        if (req.getRetirementUnit() != null) t.setRetirementUnit(trim(req.getRetirementUnit()));
        if (req.getFunctionalClass() != null) t.setFunctionalClass(trim(req.getFunctionalClass()));
        if (req.getActive() != null) t.setActive(req.getActive());

        WorkOrderTypeTemplate saved = repository.save(t);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        WorkOrderTypeTemplate t = getOrThrow(id);
        long inUse = workOrderRepository.countByWorkOrderTypeTemplate_Id(id);
        if (inUse > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Work order type is in use and cannot be deleted");
        }
        repository.delete(t);
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
                .createAsset(t.isCreateAsset())
                .propertyUnit(t.getPropertyUnit())
                .propertyGroup(t.getPropertyGroup())
                .retirementUnit(t.getRetirementUnit())
                .functionalClass(t.getFunctionalClass())
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
