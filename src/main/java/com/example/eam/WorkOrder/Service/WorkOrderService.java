package com.example.eam.WorkOrder.Service;


import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Asset.Entity.AssetWarrantyLifecycle;
import com.example.eam.Asset.Dto.AssetWarrantyLifecycleDto;
import com.example.eam.Asset.Entity.AssetLocation;
import com.example.eam.Asset.Repository.AssetLocationRepository;
import com.example.eam.Asset.Repository.AssetRepository;
import com.example.eam.Enum.*;
import com.example.eam.InventoryManagement.Entity.InventoryItem;
import com.example.eam.InventoryManagement.Repository.InventoryItemRepository;
import com.example.eam.Maintenance.Emergency.Repository.EmergencyIncidentRepository;
import com.example.eam.ServiceMaintenance.Entity.ServiceMaintenance;
import com.example.eam.ServiceMaintenance.Repository.ServiceMaintenanceRepository;
import com.example.eam.Technician.Entity.Technician;
import com.example.eam.Technician.Repository.TechnicianRepository;
import com.example.eam.TechnicianTeam.Entity.TechnicianTeam;
import com.example.eam.TechnicianTeam.Repository.TechnicianTeamRepository;
import com.example.eam.TechnicianTeam.Repository.TechnicianTeamMemberRepository;
import com.example.eam.Technician.Repository.TechnicianDailyWorkSummaryRepository;
import com.example.eam.Technician.Entity.TechnicianDailyWorkSummary;
import com.example.eam.TechnicianTeam.Entity.TechnicianTeamMember;
import com.example.eam.WorkOrder.Entity.WorkOrderTechnicianDailyLog;
import com.example.eam.WorkOrder.Repository.WorkOrderTechnicianDailyLogRepository;
import com.example.eam.WorkOrder.Dto.*;
import com.example.eam.WorkOrder.Entity.WorkOrder;
import com.example.eam.WorkOrder.Entity.WorkOrderLaborEntry;
import com.example.eam.WorkOrder.Entity.WorkOrderMaterialPlan;
import com.example.eam.WorkOrder.Entity.WorkOrderMaterialUsage;
import com.example.eam.WorkOrder.Entity.WorkOrderCheckLog;
import com.example.eam.WorkOrder.Entity.WorkOrderPauseLog;
import com.example.eam.WorkOrder.Repository.WorkOrderLaborEntryRepository;
import com.example.eam.WorkOrder.Repository.WorkOrderMaterialPlanRepository;
import com.example.eam.WorkOrder.Repository.WorkOrderMaterialUsageRepository;
import com.example.eam.WorkOrder.Repository.WorkOrderRepository;
import com.example.eam.WorkOrder.Repository.WorkOrderCheckLogRepository;
import com.example.eam.WorkOrder.Repository.WorkOrderPauseLogRepository;
import com.example.eam.WorkOrder.Repository.WorkOrderChecklistItemRepository;
import com.example.eam.WorkOrder.Dto.WorkOrderPauseWindowResponse;
import com.example.eam.Common.NotificationService;
import com.example.eam.WorkOrder.Dto.WorkOrderTeamMemberResponse;
import com.example.eam.WorkOrder.Repository.WorkOrderTypeTemplateRepository;
import com.example.eam.WorkOrder.Entity.WorkOrderTypeTemplate;
import com.example.eam.WorkRequestType.Entity.WorkRequestType;
import com.example.eam.WorkRequestType.Service.WorkRequestTypeService;
import com.example.eam.WorkOrder.Service.WoNumberPoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class WorkOrderService {
    private static final Logger log = LoggerFactory.getLogger(WorkOrderService.class);

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderChecklistItemRepository workOrderChecklistItemRepository;
    private final AssetRepository assetRepository;
    private final AssetLocationRepository assetLocationRepository;
    private final ServiceMaintenanceRepository serviceMaintenanceRepository;
    private final TechnicianRepository technicianRepository;
    private final TechnicianTeamRepository technicianTeamRepository;
    private final TechnicianTeamMemberRepository technicianTeamMemberRepository;
    private final TechnicianDailyWorkSummaryRepository technicianDailyWorkSummaryRepository;
    private final WorkOrderTechnicianDailyLogRepository workOrderTechnicianDailyLogRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final WorkOrderLaborEntryRepository workOrderLaborEntryRepository;
    private final WorkOrderMaterialUsageRepository workOrderMaterialUsageRepository;
    private final WorkOrderMaterialPlanRepository workOrderMaterialPlanRepository;
    private final WorkOrderCheckLogRepository workOrderCheckLogRepository;
    private final WorkOrderPauseLogRepository workOrderPauseLogRepository;
    private final EmergencyIncidentRepository emergencyIncidentRepository;
    private final NotificationService notificationService;
    private final WoNumberPoolService woNumberPoolService;
    private final WorkRequestTypeService workRequestTypeService;
    private final WorkOrderTypeTemplateRepository workOrderTypeTemplateRepository;

private static final Set<WorkOrderStatus> CREATION_ALLOWED_STATUSES = Set.of(
        WorkOrderStatus.NEW,
        WorkOrderStatus.APPROVED
);

private static final Map<WorkOrderStatus, Set<WorkOrderStatus>> STATUS_TRANSITIONS = Map.of(
        WorkOrderStatus.NEW, Set.of(WorkOrderStatus.APPROVED, WorkOrderStatus.REJECTED),
        WorkOrderStatus.APPROVED, Set.of(WorkOrderStatus.SCHEDULED),
        WorkOrderStatus.SCHEDULED, Set.of(WorkOrderStatus.IN_PROGRESS),
        WorkOrderStatus.IN_PROGRESS, Set.of(WorkOrderStatus.COMPLETED),
        WorkOrderStatus.COMPLETED, Set.of(WorkOrderStatus.CLOSED),
        WorkOrderStatus.REJECTED, Set.of()
);

    // ---------------- CREATE (Manual) ----------------

    @Transactional
    public WorkOrderDetailsResponse createWorkOrder(WorkOrderCreateRequest request) {

        Asset asset = null;
        if (request.getAssetId() != null) {
            asset = assetRepository.findById(request.getAssetId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));
        }

        String location = resolveLocation(asset, request.getLocation());

        // if asset is not selected, location must be provided
        if (asset == null && isBlank(location)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location is required when Asset is not selected");
        }

WorkOrderStatus status = WorkOrderStatus.NEW;
        // Always NEW on creation
        WorkRequestType workRequestType = workRequestTypeService.getOrCreateByCode(
                request.getWorkRequestTypeCode(),
                null
        );

        WorkOrderTypeTemplate woType = null;
        if (request.getWorkOrderTypeId() != null) {
            woType = workOrderTypeTemplateRepository.findById(request.getWorkOrderTypeId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Work order type not found"));
            if (!woType.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Work order type is inactive");
            }
        }

        String glAccount = trim(request.getGlAccount());
        String utilityAccount = trim(request.getUtilityAccount());
        if (glAccount == null && woType != null) glAccount = woType.getDefaultGlAccount();
        if (utilityAccount == null && woType != null) utilityAccount = woType.getDefaultUtilityAccount();

        WorkOrder wo = WorkOrder.builder()
                .workOrderId(generateUniqueWorkOrderId())
                .linkedRequest(null)
                .asset(asset)
                .workRequestType(workRequestType)
                .workOrderTypeTemplate(woType)
                .location(location)
                .workType(request.getWorkType())
                .priority(request.getPriority())
                .woTitle(request.getWoTitle())
                .descriptionScope(request.getDescriptionScope())
                .planner(null)
                .assignedTechnician(null)
                .assignedTeam(null)
                .glAccount(glAccount)
                .utilityAccount(utilityAccount)
                .plannedStartDateTime(null)
                .plannedEndDateTime(null)
                .targetCompletionDate(request.getTargetCompletionDate())
                .estimatedLaborHours(null)
                .estimatedMaterialCost(null)
                .estimatedTotalCost(null)
                .status(status)
                .source(WorkOrderSource.MANUAL)
                .deleted(false)
                .beforePhotoUrl(request.getAttachmentUrl())
                .build();

        WorkOrder saved = workOrderRepository.save(wo);
        String woNumber = woNumberPoolService.allocateWoNumber(saved.getId());
        saved.setWoNumber(woNumber);
        saved = workOrderRepository.save(saved);

        return toDetailsResponse(saved);
    }

    // ---------------- CONVERT SR -> WO ----------------

        @Transactional
public WorkOrderDetailsResponse convertServiceRequestToWorkOrder(Long serviceRequestDbId) {
    try {
        log.info("Starting conversion of Service Request ID: {}", serviceRequestDbId);
        
        ServiceMaintenance sr = serviceMaintenanceRepository.findByIdAndDeletedFalse(serviceRequestDbId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service Request not found"));

        log.info("Found Service Request: {}, Status: {}", sr.getRequestId(), sr.getStatus());

        if (sr.getStatus() != ServiceRequestStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Service Request must be APPROVED before conversion to Work Order");
        }

        // Already converted?
        if (sr.getStatus() == ServiceRequestStatus.CONVERTED_TO_WO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Service Request is already converted to Work Order");
        }
        workOrderRepository.findByLinkedRequest_Id(serviceRequestDbId).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Work Order already exists for this Service Request");
        });

        Asset asset = sr.getAsset();
        log.info("Asset: {}", asset != null ? asset.getAssetId() : "null");
        
        String location = resolveLocation(asset, sr.getLocation());
        log.info("Resolved location: {}", location);

        if (asset == null && isBlank(location)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot convert to WO: both Asset and Location are missing");
        }

        WorkType workType = mapMaintenanceTypeToWorkType(sr.getMaintenanceType());
        log.info("Mapped WorkType: {} from MaintenanceType: {}", workType, sr.getMaintenanceType());
        
        PriorityLevel priority = mapRequestPriorityToPriorityLevel(sr.getPriority());
        log.info("Mapped Priority: {} from RequestPriority: {}", priority, sr.getPriority());
        
        String woTitle = sr.getShortTitle();
        String desc = sr.getProblemDescription();
        WorkRequestType workRequestType = workRequestTypeService.getOrCreateDefaultExpenseType();

        String generatedWoId = generateUniqueWorkOrderId();
        log.info("Generated Work Order ID: {}", generatedWoId);

        WorkOrder wo = WorkOrder.builder()
        .workOrderId(generatedWoId)
        .linkedRequest(sr)
        .asset(asset)
        .workRequestType(workRequestType)
        .location(location)
        .workType(workType != null ? workType : WorkType.CORRECTIVE)
        .priority(priority != null ? priority : PriorityLevel.MEDIUM)
        .woTitle(!isBlank(woTitle) ? woTitle : "Work Order from Service Request")
        .descriptionScope(desc)
        .planner(null)
        .assignedTechnician(resolvePreferredTechnician(sr))
        .assignedTeam(resolvePreferredTeam(sr))
        .plannedStartDateTime(sr.getPreferredDateTime())
        .plannedEndDateTime(null)
        .targetCompletionDate(null)
        .estimatedLaborHours(null)
        .estimatedMaterialCost(null)
        .estimatedTotalCost(null)
        .status(WorkOrderStatus.NEW)  
        .source(WorkOrderSource.REQUEST)
        .deleted(false)
        .build();

        log.info("Saving Work Order...");
        WorkOrder saved = workOrderRepository.save(wo);
        String woNumber = woNumberPoolService.allocateWoNumber(saved.getId());
        saved.setWoNumber(woNumber);
        saved = workOrderRepository.save(saved);
        log.info("Work Order saved with ID: {}", saved.getId());

        sr.setStatus(ServiceRequestStatus.CONVERTED_TO_WO);
        sr.setLinkedWorkOrderId(saved.getWorkOrderId());
        sr.setDeleted(true);
        serviceMaintenanceRepository.save(sr);
        log.info("Service Request updated with linked Work Order ID");

        return toDetailsResponse(saved);
        
    } catch (Exception e) {
        log.error("Error converting Service Request to Work Order: ", e);
        throw e;
    }
}

    private Technician resolvePreferredTechnician(ServiceMaintenance sr) {
        if (sr.getPreferredTechnicianId() == null) return null;
        if (sr.getPreferredTeamId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Service Request has both preferredTechnicianId and preferredTeamId; cannot convert");
        }
        return resolveTechnician(sr.getPreferredTechnicianId());
    }

    private TechnicianTeam resolvePreferredTeam(ServiceMaintenance sr) {
        if (sr.getPreferredTeamId() == null) return null;
        return resolveTeam(sr.getPreferredTeamId());
    }

    // ---------------- PATCH UPDATE ----------------

    @Transactional
    public WorkOrderDetailsResponse patchWorkOrder(Long id, WorkOrderPatchRequest request) {
        WorkOrder wo = getWorkOrderOrThrow(id);
        Technician prevTechnician = wo.getAssignedTechnician();
        TechnicianTeam prevTeam = wo.getAssignedTeam();
        if (wo.getStatus() != WorkOrderStatus.NEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Work Order can only be updated while in NEW status");
        }

        // asset update
        if (request.getAssetId() != null) {
            Asset asset = assetRepository.findById(request.getAssetId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));
            wo.setAsset(asset);

            // if caller did not send location, recalc location from asset
            if (isBlank(request.getLocation())) {
                String autoLoc = resolveLocation(asset, null);
                if (!isBlank(autoLoc)) wo.setLocation(autoLoc);
            }
        }

        if (request.getLocation() != null) {
            String cleanLocation = isBlank(request.getLocation()) ? null : request.getLocation().trim();
            wo.setLocation(cleanLocation);
        }

        if (request.getWorkType() != null) wo.setWorkType(request.getWorkType());
        if (request.getPriority() != null) wo.setPriority(request.getPriority());
        if (request.getWorkRequestTypeCode() != null) {
            WorkRequestType workRequestType = workRequestTypeService.getOrCreateByCode(request.getWorkRequestTypeCode(), null);
            wo.setWorkRequestType(workRequestType);
        }

        updateIfNotNull(request.getWoTitle(), wo::setWoTitle);
        updateIfNotNull(request.getDescriptionScope(), wo::setDescriptionScope);
        updateIfNotNull(request.getPlanner(), wo::setPlanner);

        if (request.getAssignedTechnicianId() != null) {
            wo.setAssignedTechnician(resolveTechnician(request.getAssignedTechnicianId()));
        } else if (Boolean.TRUE.equals(request.getClearTechnicianAssignment())) {
            wo.setAssignedTechnician(null);
        }

        if (request.getAssignedTeamId() != null) {
            wo.setAssignedTeam(resolveTeam(request.getAssignedTeamId()));
        } else if (Boolean.TRUE.equals(request.getClearTeamAssignment())) {
            wo.setAssignedTeam(null);
        }

        updateIfNotNull(request.getPlannedStartDateTime(), wo::setPlannedStartDateTime);
        updateIfNotNull(request.getPlannedEndDateTime(), wo::setPlannedEndDateTime);
        updateIfNotNull(request.getTargetCompletionDate(), wo::setTargetCompletionDate);
        updateIfNotNull(request.getEstimatedLaborHours(), wo::setEstimatedLaborHours);
        updateIfNotNull(request.getEstimatedMaterialCost(), wo::setEstimatedMaterialCost);
        updateIfNotNull(request.getEstimatedTotalCost(), wo::setEstimatedTotalCost);

        if (request.getStatus() != null) {
            WorkOrderStatus newStatus = request.getStatus();
            if (newStatus == WorkOrderStatus.COMPLETED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use technician completion endpoint");
            }
            if (newStatus == WorkOrderStatus.CLOSED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use supervisor close endpoint");
            }
            if (newStatus == WorkOrderStatus.REJECTED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use reject endpoint");
            }
            validateStatusTransition(wo.getStatus(), newStatus);
            handleStatusSideEffects(wo, newStatus);
            wo.setStatus(newStatus);
        }

        if (request.getSource() != null) wo.setSource(request.getSource());

        if (wo.getAsset() == null && isBlank(wo.getLocation())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Location is required when Asset is not selected");
        }

        WorkOrder saved = workOrderRepository.save(wo);
        return toDetailsResponse(saved);
    }

    @Transactional
    public WorkOrderDetailsResponse approveWorkOrder(Long id, WorkOrderApproveRequest request) {
        WorkOrder wo = getWorkOrderOrThrow(id);
        if (wo.getStatus() != WorkOrderStatus.NEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only NEW work orders can be approved");
        }

        updateIfNotNull(request.getEstimatedLaborHours(), wo::setEstimatedLaborHours);
        updateIfNotNull(request.getEstimatedMaterialCost(), wo::setEstimatedMaterialCost);

        wo.setApprovalNotes(trim(request.getApprovalNotes()));
        wo.setApprovedBy(trim(request.getApprovedBy()));
        wo.setApprovedAt(LocalDateTime.now());

        validateStatusTransition(wo.getStatus(), WorkOrderStatus.APPROVED);
        wo.setStatus(WorkOrderStatus.APPROVED);

        // set accounting for labor/material from request or template defaults
        WorkOrderTypeTemplate woType = wo.getWorkOrderTypeTemplate();
        if (request.getLaborGlAccount() != null) {
            wo.setLaborGlAccount(trim(request.getLaborGlAccount()));
        } else if (woType != null && wo.getLaborGlAccount() == null) {
            wo.setLaborGlAccount(woType.getLaborGlAccount());
        }
        if (request.getLaborUtilityAccount() != null) {
            wo.setLaborUtilityAccount(trim(request.getLaborUtilityAccount()));
        } else if (woType != null && wo.getLaborUtilityAccount() == null) {
            wo.setLaborUtilityAccount(woType.getLaborUtilityAccount());
        }
        if (request.getInventoryGlAccount() != null) {
            wo.setInventoryGlAccount(trim(request.getInventoryGlAccount()));
        } else if (woType != null && wo.getInventoryGlAccount() == null) {
            wo.setInventoryGlAccount(woType.getInventoryGlAccount());
        }
        if (request.getInventoryUtilityAccount() != null) {
            wo.setInventoryUtilityAccount(trim(request.getInventoryUtilityAccount()));
        } else if (woType != null && wo.getInventoryUtilityAccount() == null) {
            wo.setInventoryUtilityAccount(woType.getInventoryUtilityAccount());
        }

        WorkOrder saved = workOrderRepository.save(wo);
        return toDetailsResponse(saved);
    }

    @Transactional
    public WorkOrderDetailsResponse rejectWorkOrder(Long id, WorkOrderRejectRequest request) {
        WorkOrder wo = getWorkOrderOrThrow(id);
        if (wo.getStatus() != WorkOrderStatus.NEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only NEW work orders can be rejected");
        }

        wo.setRejectionReason(trim(request.getRejectionReason()));
        wo.setRejectedBy(trim(request.getRejectedBy()));
        wo.setRejectedAt(LocalDateTime.now());

        // Clear any previous approval metadata if present
        wo.setApprovalNotes(null);
        wo.setApprovedBy(null);
        wo.setApprovedAt(null);

        validateStatusTransition(wo.getStatus(), WorkOrderStatus.REJECTED);
        wo.setStatus(WorkOrderStatus.REJECTED);

        WorkOrder saved = workOrderRepository.save(wo);
        return toDetailsResponse(saved);
    }

    @Transactional
    public WorkOrderDetailsResponse scheduleWorkOrder(Long id, WorkOrderScheduleRequest request) {
        WorkOrder wo = getWorkOrderOrThrow(id);
        if (wo.getStatus() != WorkOrderStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only APPROVED work orders can be scheduled");
        }
        if (request.getAssignedTechnicianId() == null && request.getAssignedTeamId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assign technician or team to schedule work order");
        }
        if (request.getPlannedEndDateTime().isBefore(request.getPlannedStartDateTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "plannedEndDateTime must be after plannedStartDateTime");
        }

        if (request.getAssignedTechnicianId() != null) {
            wo.setAssignedTechnician(resolveTechnician(request.getAssignedTechnicianId()));
        }
        if (request.getAssignedTeamId() != null) {
            wo.setAssignedTeam(resolveTeam(request.getAssignedTeamId()));
        }
        wo.setPlanner(trim(request.getPlanner()));
        wo.setPlannedStartDateTime(request.getPlannedStartDateTime());
        wo.setPlannedEndDateTime(request.getPlannedEndDateTime());
        wo.setPrecheckNotes(trim(request.getPreCheckNotes()));

        // replace planned materials if provided
        if (request.getPlannedMaterials() != null) {
            workOrderMaterialPlanRepository.deleteByWorkOrder(wo);
            List<WorkOrderMaterialPlan> plans = request.getPlannedMaterials().stream()
                    .map(pm -> buildMaterialPlan(wo, pm))
                    .toList();
            workOrderMaterialPlanRepository.saveAll(plans);
        }

        validateStatusTransition(wo.getStatus(), WorkOrderStatus.SCHEDULED);
        handleStatusSideEffects(wo, WorkOrderStatus.SCHEDULED);
        wo.setStatus(WorkOrderStatus.SCHEDULED);

        WorkOrder saved = workOrderRepository.save(wo);
        notifyAssignment(saved);
        return toDetailsResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AvailabilitySlotResponse> getTechnicianAvailability(Long technicianId, WorkOrderAvailabilityRequest request) {
        Technician technician = resolveTechnician(technicianId);
        return computeAvailabilitySlots(technicianId, null, technician, null, request);
    }

    @Transactional(readOnly = true)
    public List<AvailabilitySlotResponse> getTeamAvailability(Long teamId, WorkOrderAvailabilityRequest request) {
        TechnicianTeam team = resolveTeam(teamId);
        return computeAvailabilitySlots(null, teamId, null, team, request);
    }

    @Transactional
    public WorkOrderDetailsResponse markInProgress(Long id, WorkOrderInProgressRequest request) {
        WorkOrder wo = getWorkOrderOrThrow(id);
        if (wo.getStatus() != WorkOrderStatus.SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only SCHEDULED work orders can be moved to IN_PROGRESS");
        }

        if (request != null && request.getActualStartDateTime() != null) {
            wo.setActualStartDateTime(request.getActualStartDateTime());
        }

        validateStatusTransition(wo.getStatus(), WorkOrderStatus.IN_PROGRESS);
        handleStatusSideEffects(wo, WorkOrderStatus.IN_PROGRESS);
        wo.setStatus(WorkOrderStatus.IN_PROGRESS);

        WorkOrder saved = workOrderRepository.save(wo);
        return toDetailsResponse(saved);
    }

    @Transactional(readOnly = true)
    public WorkOrderListResponse listScheduledForTechnician(Long technicianId, Pageable pageable) {
        if (technicianId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "technicianId is required");
        }

        Page<WorkOrder> page = workOrderRepository.findByTechnicianOrTeamMember(
                technicianId,
                pageable
        );

        List<WorkOrderDetailsResponse> rows = page.getContent().stream()
                .map(this::toDetailsResponse)
                .toList();

        return WorkOrderListResponse.builder()
                .workOrders(rows)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional
    public WorkOrderDetailsResponse checkIn(Long id, WorkOrderCheckInRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Check-in payload is required");
        }
        WorkOrder wo = getWorkOrderOrThrow(id);
        WorkOrderStatus status = wo.getStatus();
        if (status == WorkOrderStatus.CLOSED || status == WorkOrderStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Check-in not allowed once work order is closed or rejected");
        }

        workOrderCheckLogRepository.findFirstByWorkOrder_IdAndCheckOutAtIsNullOrderByCheckInAtDesc(id)
                .ifPresent(open -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "An open check-in already exists for this work order");
                });

        Technician technician = resolveTechnician(request.getTechnicianId());
        TechnicianTeam team = resolveTeam(request.getTeamId());
        TechnicianTeam assignedTeam = wo.getAssignedTeam();
        if (assignedTeam != null) {
            if (team == null) {
                team = assignedTeam;
            } else if (!assignedTeam.getId().equals(team.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team must match the work order assigned team");
            }
        }
        if (wo.getAssignedTechnician() != null && technician != null
                && !wo.getAssignedTechnician().getId().equals(technician.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Technician must match the work order assignment");
        }
        if (team == null && technician == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide technicianId or teamId for check-in");
        }
        if (team != null && technician != null) {
            boolean member = getTeamMembers(team).stream()
                    .anyMatch(t -> t.getId().equals(technician.getId()));
            if (!member) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Technician is not part of the specified team");
            }
        }

        LocalDateTime checkIn = request.getCheckInAt() != null
                ? request.getCheckInAt()
                : LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        WorkOrderCheckLog log = WorkOrderCheckLog.builder()
                .workOrder(wo)
                .technician(technician)
                .team(team)
                .checkInAt(checkIn)
                .checkOutAt(null)
                .notes(trim(request.getNotes()))
                .build();
        workOrderCheckLogRepository.save(log);

        if (wo.getActualStartDateTime() == null || checkIn.isBefore(wo.getActualStartDateTime())) {
            wo.setActualStartDateTime(checkIn);
        }

        WorkOrder saved = workOrderRepository.save(wo);
        return toDetailsResponse(saved);
    }

    @Transactional
    public WorkOrderDetailsResponse pause(Long id, WorkOrderPauseRequest request) {
        WorkOrder wo = getWorkOrderOrThrow(id);
        if (wo.getStatus() != WorkOrderStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Work Order must be IN_PROGRESS to pause");
        }

        LocalDateTime pauseAt = request != null && request.getPauseAt() != null
                ? request.getPauseAt()
                : LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        List<WorkOrderCheckLog> openLogs = workOrderCheckLogRepository.findByWorkOrder_IdAndCheckOutAtIsNull(id);
        if (openLogs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No open check-in found for this work order");
        }
        if (openLogs.size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Multiple open sessions found; use team pause endpoint");
        }
        WorkOrderCheckLog openLog = openLogs.get(0);
        workOrderPauseLogRepository.findFirstByCheckLog_IdAndResumeAtIsNullOrderByPauseAtDesc(openLog.getId())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Work order is already paused");
                });
        if (pauseAt.isBefore(openLog.getCheckInAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pauseAt cannot be before checkInAt");
        }

        WorkOrderPauseLog pauseLog = WorkOrderPauseLog.builder()
                .checkLog(openLog)
                .pauseAt(pauseAt)
                .resumeAt(null)
                .build();
        openLog.getPauseLogs().add(pauseLog);

        if (request != null && request.getNotes() != null) {
            openLog.setNotes(trim(request.getNotes()));
        }
        workOrderCheckLogRepository.save(openLog);

        WorkOrder saved = workOrderRepository.save(wo);
        return toDetailsResponse(saved);
    }

    @Transactional
    public WorkOrderDetailsResponse resume(Long id, WorkOrderResumeRequest request) {
        WorkOrder wo = getWorkOrderOrThrow(id);
        if (wo.getStatus() != WorkOrderStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Work Order must be IN_PROGRESS to resume");
        }

        List<WorkOrderCheckLog> openLogs = workOrderCheckLogRepository.findByWorkOrder_IdAndCheckOutAtIsNull(id);
        if (openLogs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No open check-in found for this work order");
        }
        WorkOrderCheckLog openLog;
        if (openLogs.size() > 1) {
            Long technicianId = request != null ? request.getTechnicianId() : null;
            if (technicianId == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Multiple open sessions found; specify technicianId or use team resume");
            }
            openLog = openLogs.stream()
                    .filter(log -> log.getTechnician() != null && technicianId.equals(log.getTechnician().getId()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No open check-in found for the specified technician"));
        } else {
            openLog = openLogs.get(0);
        }
        WorkOrderPauseLog pauseLog = workOrderPauseLogRepository
                .findFirstByCheckLog_IdAndResumeAtIsNullOrderByPauseAtDesc(openLog.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Work order is not paused"));

        LocalDateTime resumeAt = request != null && request.getResumeAt() != null
                ? request.getResumeAt()
                : LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        if (resumeAt.isBefore(pauseLog.getPauseAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resumeAt cannot be before pauseAt");
        }

        Technician technician = resolveTechnician(request != null ? request.getTechnicianId() : null);
        TechnicianTeam team = resolveTeam(request != null ? request.getTeamId() : null);

        if (technician != null && openLog.getTechnician() != null
                && !technician.getId().equals(openLog.getTechnician().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change technician during an open session");
        }
        if (team != null && openLog.getTeam() != null
                && !team.getId().equals(openLog.getTeam().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change team during an open session");
        }
        if (team != null && technician != null) {
            boolean member = getTeamMembers(team).stream()
                    .anyMatch(t -> t.getId().equals(technician.getId()));
            if (!member) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Technician is not part of the specified team");
            }
        }

        pauseLog.setResumeAt(resumeAt);
        if (technician != null) openLog.setTechnician(technician);
        if (team != null) openLog.setTeam(team);
        if (request != null && request.getNotes() != null) {
            openLog.setNotes(trim(request.getNotes()));
        }
        workOrderCheckLogRepository.save(openLog);

        WorkOrder saved = workOrderRepository.save(wo);
        return toDetailsResponse(saved);
    }

    @Transactional
    public WorkOrderDetailsResponse checkOut(Long id, WorkOrderCheckOutRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Check-out payload is required");
        }
        WorkOrder wo = getWorkOrderOrThrow(id);
        WorkOrderStatus status = wo.getStatus();
        if (status == WorkOrderStatus.CLOSED || status == WorkOrderStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Check-out not allowed once work order is closed or rejected");
        }

        List<WorkOrderCheckLog> openLogs = workOrderCheckLogRepository.findByWorkOrder_IdAndCheckOutAtIsNull(id);
        if (openLogs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No open check-in found for this work order");
        }
        WorkOrderCheckLog openLog;
        if (openLogs.size() > 1) {
            if (request.getTechnicianId() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Multiple open sessions found; specify technicianId or use team checkout");
            }
            openLog = openLogs.stream()
                    .filter(log -> log.getTechnician() != null && request.getTechnicianId().equals(log.getTechnician().getId()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No open check-in found for the specified technician"));
        } else {
            openLog = openLogs.get(0);
        }

        LocalDateTime checkOut = request.getCheckOutAt() != null
                ? request.getCheckOutAt()
                : LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        if (checkOut.isBefore(openLog.getCheckInAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "checkOutAt cannot be before checkInAt");
        }
        workOrderPauseLogRepository.findFirstByCheckLog_IdAndResumeAtIsNullOrderByPauseAtDesc(openLog.getId())
                .ifPresent(pl -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Resume before checking out");
                });
        openLog.setCheckOutAt(checkOut);
        if (request.getNotes() != null) {
            openLog.setNotes(trim(request.getNotes()));
        }
        workOrderCheckLogRepository.save(openLog);

        accumulateLaborHours(wo, openLog);
        updateTechnicianDailyWorkSummaries(openLog);

        if (wo.getActualEndDateTime() == null || checkOut.isAfter(wo.getActualEndDateTime())) {
            wo.setActualEndDateTime(checkOut);
        }

        WorkOrder saved = workOrderRepository.save(wo);
        return toDetailsResponse(saved);
    }

    @Transactional
    public WorkOrderDetailsResponse teamCheckIn(Long id, WorkOrderTeamCheckInRequest request) {
        if (request == null || request.getTechnicians() == null || request.getTechnicians().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team check-in payload is required");
        }

        WorkOrder wo = getWorkOrderOrThrow(id);
        ensureCheckFlowAllowed(wo);
        TechnicianTeam team = requireAssignedTeam(wo, request.getTeamId());
        Map<Long, Technician> membersById = mapTeamMembersById(team);
        Set<Long> seen = new java.util.HashSet<>();
        LocalDateTime earliestCheckIn = wo.getActualStartDateTime();

        for (WorkOrderTeamCheckInEntry entry : request.getTechnicians()) {
            if (entry == null) continue;
            Long technicianId = entry.getTechnicianId();
            if (technicianId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "technicianId is required for each check-in entry");
            }
            if (!seen.add(technicianId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate technicianId in team check-in: " + technicianId);
            }
            Technician technician = membersById.get(technicianId);
            if (technician == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Technician " + technicianId + " is not part of the team");
            }
            workOrderCheckLogRepository.findFirstByWorkOrder_IdAndTechnician_IdAndCheckOutAtIsNullOrderByCheckInAtDesc(id, technicianId)
                    .ifPresent(open -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Technician " + technicianId + " already has an open check-in for this work order");
                    });

            LocalDateTime checkIn = entry.getCheckInAt() != null
                    ? entry.getCheckInAt()
                    : LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

            WorkOrderCheckLog log = WorkOrderCheckLog.builder()
                    .workOrder(wo)
                    .technician(technician)
                    .team(team)
                    .checkInAt(checkIn)
                    .checkOutAt(null)
                    .notes(trim(entry.getNotes()))
                    .build();
            workOrderCheckLogRepository.save(log);

            if (earliestCheckIn == null || checkIn.isBefore(earliestCheckIn)) {
                earliestCheckIn = checkIn;
            }
        }

        if (earliestCheckIn != null && (wo.getActualStartDateTime() == null || earliestCheckIn.isBefore(wo.getActualStartDateTime()))) {
            wo.setActualStartDateTime(earliestCheckIn);
        }

        WorkOrder saved = workOrderRepository.save(wo);
        return toDetailsResponse(saved);
    }

    @Transactional
    public WorkOrderDetailsResponse teamCheckOut(Long id, WorkOrderTeamCheckOutRequest request) {
        if (request == null || request.getTechnicians() == null || request.getTechnicians().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team check-out payload is required");
        }

        WorkOrder wo = getWorkOrderOrThrow(id);
        ensureCheckFlowAllowed(wo);
        TechnicianTeam team = requireAssignedTeam(wo, request.getTeamId());
        Map<Long, Technician> membersById = mapTeamMembersById(team);
        Set<Long> seen = new java.util.HashSet<>();
        LocalDateTime latestCheckOut = wo.getActualEndDateTime();

        for (WorkOrderTeamCheckOutEntry entry : request.getTechnicians()) {
            if (entry == null) continue;
            Long technicianId = entry.getTechnicianId();
            if (technicianId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "technicianId is required for each check-out entry");
            }
            if (!seen.add(technicianId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate technicianId in team check-out: " + technicianId);
            }
            Technician technician = membersById.get(technicianId);
            if (technician == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Technician " + technicianId + " is not part of the team");
            }

            WorkOrderCheckLog openLog = workOrderCheckLogRepository.requireOpenLogForTechnician(id, technicianId);
            if (openLog.getTeam() != null && !openLog.getTeam().getId().equals(team.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Open check-in for technician " + technicianId + " is linked to a different team");
            }
            LocalDateTime checkOut = entry.getCheckOutAt() != null
                    ? entry.getCheckOutAt()
                    : LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
            if (checkOut.isBefore(openLog.getCheckInAt())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "checkOutAt cannot be before checkInAt for technician " + technicianId);
            }
            workOrderPauseLogRepository.findFirstByCheckLog_IdAndResumeAtIsNullOrderByPauseAtDesc(openLog.getId())
                    .ifPresent(pl -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Resume before checking out for technician " + technicianId);
                    });

            openLog.setCheckOutAt(checkOut);
            if (entry.getNotes() != null) {
                openLog.setNotes(trim(entry.getNotes()));
            }
            workOrderCheckLogRepository.save(openLog);

            accumulateLaborHours(wo, openLog);
            updateTechnicianDailyWorkSummaries(openLog);

            if (latestCheckOut == null || checkOut.isAfter(latestCheckOut)) {
                latestCheckOut = checkOut;
            }
        }

        if (latestCheckOut != null && (wo.getActualEndDateTime() == null || latestCheckOut.isAfter(wo.getActualEndDateTime()))) {
            wo.setActualEndDateTime(latestCheckOut);
        }

        WorkOrder saved = workOrderRepository.save(wo);
        return toDetailsResponse(saved);
    }

    @Transactional
    public WorkOrderDetailsResponse teamPause(Long id, WorkOrderTeamPauseRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team pause payload is required");
        }
        WorkOrder wo = getWorkOrderOrThrow(id);
        ensureCheckFlowAllowed(wo);
        TechnicianTeam team = requireAssignedTeam(wo, request.getTeamId());
        if (wo.getStatus() != WorkOrderStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Work Order must be IN_PROGRESS to pause");
        }

        List<WorkOrderCheckLog> openLogs = workOrderCheckLogRepository.findByWorkOrder_IdAndCheckOutAtIsNull(id);
        if (openLogs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No open check-in found for this work order");
        }

        LocalDateTime pauseAt = request.getPauseAt() != null
                ? request.getPauseAt()
                : LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        for (WorkOrderCheckLog openLog : openLogs) {
            if (openLog.getTeam() != null && !openLog.getTeam().getId().equals(team.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Open check-in is linked to a different team");
            }
            workOrderPauseLogRepository.findFirstByCheckLog_IdAndResumeAtIsNullOrderByPauseAtDesc(openLog.getId())
                    .ifPresent(existing -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Work order is already paused for at least one technician");
                    });
            if (pauseAt.isBefore(openLog.getCheckInAt())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pauseAt cannot be before checkInAt");
            }

            WorkOrderPauseLog pauseLog = WorkOrderPauseLog.builder()
                    .checkLog(openLog)
                    .pauseAt(pauseAt)
                    .resumeAt(null)
                    .build();
            if (openLog.getPauseLogs() == null) {
                openLog.setPauseLogs(new ArrayList<>());
            }
            openLog.getPauseLogs().add(pauseLog);

            if (request.getNotes() != null) {
                openLog.setNotes(trim(request.getNotes()));
            }
            workOrderCheckLogRepository.save(openLog);
        }

        WorkOrder saved = workOrderRepository.save(wo);
        return toDetailsResponse(saved);
    }

    @Transactional
    public WorkOrderDetailsResponse teamResume(Long id, WorkOrderTeamResumeRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team resume payload is required");
        }
        WorkOrder wo = getWorkOrderOrThrow(id);
        ensureCheckFlowAllowed(wo);
        TechnicianTeam team = requireAssignedTeam(wo, request.getTeamId());
        if (wo.getStatus() != WorkOrderStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Work Order must be IN_PROGRESS to resume");
        }

        List<WorkOrderCheckLog> openLogs = workOrderCheckLogRepository.findByWorkOrder_IdAndCheckOutAtIsNull(id);
        if (openLogs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No open check-in found for this work order");
        }

        LocalDateTime resumeAt = request.getResumeAt() != null
                ? request.getResumeAt()
                : LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        for (WorkOrderCheckLog openLog : openLogs) {
            if (openLog.getTeam() != null && !openLog.getTeam().getId().equals(team.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Open check-in is linked to a different team");
            }
            WorkOrderPauseLog pauseLog = workOrderPauseLogRepository
                    .findFirstByCheckLog_IdAndResumeAtIsNullOrderByPauseAtDesc(openLog.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Work order is not paused"));

            if (resumeAt.isBefore(pauseLog.getPauseAt())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resumeAt cannot be before pauseAt");
            }

            pauseLog.setResumeAt(resumeAt);
            if (request.getNotes() != null) {
                openLog.setNotes(trim(request.getNotes()));
            }
            workOrderCheckLogRepository.save(openLog);
        }

        WorkOrder saved = workOrderRepository.save(wo);
        return toDetailsResponse(saved);
    }

    // ---------------- COMPLETION / CLOSE FLOW ----------------

    @Transactional
    public WorkOrderDetailsResponse recordTechnicianCompletion(Long id, WorkOrderCompletionRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completion payload is required");
        }

        WorkOrder wo = getWorkOrderOrThrow(id);
        if (wo.getStatus() != WorkOrderStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only in-progress work orders can be completed by technicians");
        }

        if (request.getActualStartDateTime() != null) {
            wo.setActualStartDateTime(request.getActualStartDateTime());
        } else if (wo.getActualStartDateTime() == null) {
            wo.setActualStartDateTime(LocalDateTime.now());
        }

        if (request.getActualEndDateTime() != null) {
            wo.setActualEndDateTime(request.getActualEndDateTime());
        } else {
            wo.setActualEndDateTime(LocalDateTime.now());
        }

        updateIfNotNull(request.getCompletionNotes(), wo::setCompletionNotes);
        updateIfNotNull(request.getFailureCause(), wo::setFailureCause);
        updateIfNotNull(request.getRemedyAction(), wo::setRemedyAction);
        updateIfNotNull(request.getBeforePhotoUrl(), wo::setBeforePhotoUrl);
        updateIfNotNull(request.getAfterPhotoUrl(), wo::setAfterPhotoUrl);

        BigDecimal totalLaborHours = BigDecimal.ZERO;
        BigDecimal totalLaborCost = BigDecimal.ZERO;
        if (request.getLaborEntries() != null) {
            for (WorkOrderLaborEntryRequest laborRequest : request.getLaborEntries()) {
                WorkOrderLaborEntry entry = buildLaborEntry(wo, laborRequest);
                workOrderLaborEntryRepository.save(entry);
                totalLaborHours = totalLaborHours.add(entry.getLaborHours());
                if (entry.getLaborCost() != null) {
                    totalLaborCost = totalLaborCost.add(entry.getLaborCost());
                }
            }
        }

        if (totalLaborHours.compareTo(BigDecimal.ZERO) > 0) {
            if (wo.getActualLaborHours() == null) {
                wo.setActualLaborHours(totalLaborHours);
            } else {
                wo.setActualLaborHours(wo.getActualLaborHours().add(totalLaborHours));
            }
        }
        wo.setActualLaborCost(totalLaborCost.compareTo(BigDecimal.ZERO) > 0 ? totalLaborCost : null);

        BigDecimal totalMaterialCost = BigDecimal.ZERO;
        if (request.getMaterialsUsed() != null) {
            for (WorkOrderMaterialUsageRequest usageRequest : request.getMaterialsUsed()) {
                WorkOrderMaterialUsage usage = buildMaterialUsage(wo, usageRequest);
                workOrderMaterialUsageRepository.save(usage);
                if (usage.getTotalCostSnapshot() != null) {
                    totalMaterialCost = totalMaterialCost.add(usage.getTotalCostSnapshot());
                }
            }
        }

        wo.setActualMaterialCost(totalMaterialCost.compareTo(BigDecimal.ZERO) > 0 ? totalMaterialCost : null);
        wo.setActualTotalCost(addCosts(wo.getActualLaborCost(), wo.getActualMaterialCost()));

        validateStatusTransition(wo.getStatus(), WorkOrderStatus.COMPLETED);
        wo.setStatus(WorkOrderStatus.COMPLETED);

        WorkOrder saved = workOrderRepository.save(wo);
        return toDetailsResponse(saved);
    }

    @Transactional
    public WorkOrderDetailsResponse closeWorkOrder(Long id, WorkOrderCloseRequest request) {
        WorkOrder wo = getWorkOrderOrThrow(id);
        if (wo.getStatus() != WorkOrderStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only completed work orders can be closed");
        }
        if (request != null) {
            updateIfNotNull(request.getSupervisorNotes(), wo::setSupervisorNotes);
        }
        validateStatusTransition(wo.getStatus(), WorkOrderStatus.CLOSED);
        wo.setStatus(WorkOrderStatus.CLOSED);
        WorkOrder saved = workOrderRepository.save(wo);
        return toDetailsResponse(saved);
    }

    // ---------------- READ ----------------

    @Transactional(readOnly = true)
    public WorkOrderDetailsResponse getWorkOrderDetails(Long id) {
        WorkOrder wo = getWorkOrderOrThrow(id);
        return toDetailsResponse(wo);
    }

    @Transactional(readOnly = true)
    public WorkOrderListResponse listWorkOrders(Pageable pageable) {
        Pageable effectivePageable = pageable;
        if (pageable.getSort() == null || pageable.getSort().isUnsorted()) {
            effectivePageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by("plannedEndDateTime").ascending().and(Sort.by("id").ascending())
            );
        }
        Page<WorkOrder> page = workOrderRepository.findByDeletedFalse(effectivePageable);
        List<WorkOrderDetailsResponse> rows = page.getContent().stream()
                .map(this::toDetailsResponse)
                .toList();

        return WorkOrderListResponse.builder()
                .workOrders(rows)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    // ---------------- DELETE (soft delete) ----------------

    @Transactional
    public void deleteWorkOrder(Long id) {
        WorkOrder wo = getWorkOrderOrThrow(id);
        if (wo.getStatus() != WorkOrderStatus.NEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only NEW work orders can be deleted");
        }
        wo.setDeleted(true);
        workOrderRepository.save(wo);
    }

    // ---------------- Helpers ----------------

    private WorkOrder getWorkOrderOrThrow(Long id) {
        return workOrderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work Order not found"));
    }

    private <T> void updateIfNotNull(T value, Consumer<T> setter) {
        if (value != null) setter.accept(value);
    }

    private String resolveLocation(Asset asset, String providedLocation) {
        if (!isBlank(providedLocation)) return providedLocation.trim();
        if (asset == null) return null;

        return assetLocationRepository.findByAsset_Id(asset.getId())
                .map(AssetLocation::getLocation)
                .filter(v -> !isBlank(v))
                .orElse(null);
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

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void validateStatusTransition(WorkOrderStatus current, WorkOrderStatus requested) {
        if (requested == null || current == requested) return;
        Set<WorkOrderStatus> allowed = STATUS_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(requested)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Cannot move Work Order from %s to %s", current, requested));
        }
    }

    private void handleStatusSideEffects(WorkOrder wo, WorkOrderStatus newStatus) {
        if (newStatus == WorkOrderStatus.IN_PROGRESS && wo.getActualStartDateTime() == null) {
            wo.setActualStartDateTime(LocalDateTime.now());
        }
    }

    private void ensureCheckFlowAllowed(WorkOrder wo) {
        WorkOrderStatus status = wo.getStatus();
        if (status == WorkOrderStatus.CLOSED || status == WorkOrderStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Operation not allowed once work order is closed or rejected");
        }
    }

    private TechnicianTeam requireAssignedTeam(WorkOrder wo, Long teamId) {
        if (teamId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "teamId is required");
        }
        TechnicianTeam assignedTeam = wo.getAssignedTeam();
        if (assignedTeam == null || assignedTeam.getId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Work order is not assigned to a team");
        }
        TechnicianTeam requestedTeam = resolveTeam(teamId);
        if (requestedTeam == null || requestedTeam.getId() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found: " + teamId);
        }
        if (!assignedTeam.getId().equals(requestedTeam.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Specified team does not match the work order assignment");
        }
        return requestedTeam;
    }

    private List<WorkOrderPauseLog> resolvePauseLogs(WorkOrderCheckLog log) {
        List<WorkOrderPauseLog> pauses = log.getPauseLogs();
        if (pauses == null || pauses.isEmpty()) {
            pauses = workOrderPauseLogRepository.findByCheckLog_IdOrderByPauseAtAsc(log.getId());
        }
        return pauses;
    }

    private void accumulateLaborHours(WorkOrder wo, WorkOrderCheckLog log) {
        if (log.getCheckInAt() == null || log.getCheckOutAt() == null) return;

        long workingSeconds = computeWorkingSeconds(log);
        if (workingSeconds <= 0) return;

        BigDecimal hours = BigDecimal.valueOf(workingSeconds)
                .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);
        if (hours.compareTo(BigDecimal.ZERO) <= 0) return;
        if (wo.getActualLaborHours() == null) {
            wo.setActualLaborHours(hours);
        } else {
            wo.setActualLaborHours(wo.getActualLaborHours().add(hours));
        }
    }

    private void updateTechnicianDailyWorkSummaries(WorkOrderCheckLog checkLog) {
        if (checkLog == null || checkLog.getCheckInAt() == null || checkLog.getCheckOutAt() == null) return;

        Map<LocalDate, Long> secondsByDate = computeWorkingSecondsByDate(checkLog);
        if (secondsByDate.isEmpty()) return;

        List<Technician> technicians = resolveTechniciansForLog(checkLog);
        if (technicians.isEmpty()) {
            log.warn("No technician or team linked to check log {}", checkLog.getId());
            return;
        }

        WorkOrder workOrder = checkLog.getWorkOrder();
        for (Technician tech : technicians) {
            if (tech == null || tech.getId() == null) continue;
            for (Map.Entry<LocalDate, Long> entry : secondsByDate.entrySet()) {
                TechnicianDailyWorkSummary summary = technicianDailyWorkSummaryRepository
                        .findByTechnician_IdAndWorkDate(tech.getId(), entry.getKey())
                        .orElseGet(() -> TechnicianDailyWorkSummary.builder()
                                .technician(tech)
                                .workDate(entry.getKey())
                                .workingSeconds(0L)
                                .build());
                summary.setWorkingSeconds(summary.getWorkingSeconds() + entry.getValue());
                technicianDailyWorkSummaryRepository.save(summary);

                if (workOrder != null && workOrder.getId() != null) {
                    WorkOrderTechnicianDailyLog woLog = workOrderTechnicianDailyLogRepository
                            .findByWorkOrder_IdAndTechnician_IdAndWorkDate(workOrder.getId(), tech.getId(), entry.getKey())
                            .orElseGet(() -> WorkOrderTechnicianDailyLog.builder()
                                    .workOrder(workOrder)
                                    .technician(tech)
                                    .workDate(entry.getKey())
                                    .workingSeconds(0L)
                                    .build());
                    woLog.setWorkingSeconds(woLog.getWorkingSeconds() + entry.getValue());
                    workOrderTechnicianDailyLogRepository.save(woLog);
                }
            }
        }
    }

    private Map<LocalDate, Long> computeWorkingSecondsByDate(WorkOrderCheckLog checkLog) {
        if (checkLog.getCheckInAt() == null || checkLog.getCheckOutAt() == null) return Map.of();

        List<TimeInterval> intervals = buildActiveIntervals(checkLog);
        Map<LocalDate, Long> secondsByDate = new HashMap<>();
        for (TimeInterval interval : intervals) {
            LocalDateTime cursor = interval.start();
            LocalDateTime end = interval.end();
            while (cursor.isBefore(end)) {
                LocalDateTime dayBoundary = cursor.toLocalDate().plusDays(1).atStartOfDay();
                LocalDateTime sliceEnd = end.isBefore(dayBoundary) ? end : dayBoundary;
                long seconds = ChronoUnit.SECONDS.between(cursor, sliceEnd);
                if (seconds > 0) {
                    secondsByDate.merge(cursor.toLocalDate(), seconds, Long::sum);
                }
                cursor = sliceEnd;
            }
        }
        return secondsByDate;
    }

    private List<TimeInterval> buildActiveIntervals(WorkOrderCheckLog checkLog) {
        if (checkLog.getCheckInAt() == null || checkLog.getCheckOutAt() == null) return List.of();

        LocalDateTime checkIn = checkLog.getCheckInAt();
        LocalDateTime checkOut = checkLog.getCheckOutAt();
        List<WorkOrderPauseLog> pauses = new ArrayList<>(resolvePauseLogs(checkLog));
        pauses.sort(Comparator.comparing(WorkOrderPauseLog::getPauseAt, Comparator.nullsLast(Comparator.naturalOrder())));

        List<TimeInterval> intervals = new ArrayList<>();
        LocalDateTime cursor = checkIn;
        for (WorkOrderPauseLog pause : pauses) {
            if (pause.getPauseAt() == null || pause.getResumeAt() == null) continue;
            if (pause.getPauseAt().isAfter(checkOut)) break;
            LocalDateTime intervalEnd = pause.getPauseAt().isBefore(checkOut) ? pause.getPauseAt() : checkOut;
            if (cursor.isBefore(intervalEnd)) {
                intervals.add(new TimeInterval(cursor, intervalEnd));
            }
            cursor = pause.getResumeAt();
            if (cursor.isAfter(checkOut)) {
                cursor = checkOut;
                break;
            }
        }
        if (cursor.isBefore(checkOut)) {
            intervals.add(new TimeInterval(cursor, checkOut));
        }
        return intervals;
    }

    private List<Technician> resolveTechniciansForLog(WorkOrderCheckLog log) {
        if (log == null) return List.of();
        if (log.getTeam() != null) {
            return dedupeById(getTeamMembers(log.getTeam()));
        }
        if (log.getTechnician() != null) {
            return List.of(log.getTechnician());
        }
        return List.of();
    }

    private List<Technician> getTeamMembers(TechnicianTeam team) {
        if (team == null || team.getId() == null) return List.of();
        return technicianTeamMemberRepository.findByTeam_Id(team.getId()).stream()
                .map(TechnicianTeamMember::getTechnician)
                .filter(Objects::nonNull)
                .toList();
    }

    private Map<Long, Technician> mapTeamMembersById(TechnicianTeam team) {
        Map<Long, Technician> byId = new LinkedHashMap<>();
        for (Technician tech : dedupeById(getTeamMembers(team))) {
            if (tech == null || tech.getId() == null) continue;
            byId.putIfAbsent(tech.getId(), tech);
        }
        return byId;
    }

    private List<WorkOrderTeamMemberResponse> toTeamMemberResponses(TechnicianTeam team) {
        if (team == null || team.getId() == null) return List.of();
        return technicianTeamMemberRepository.findByTeam_Id(team.getId()).stream()
                .map(member -> {
                    Technician tech = member.getTechnician();
                    if (tech == null) return null;
                    return WorkOrderTeamMemberResponse.builder()
                            .technicianId(tech.getId())
                            .technicianName(resolveTechnicianName(tech))
                            .email(tech.getEmail())
                            .teamLeader(member.isTeamLeader())
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private List<Technician> dedupeById(List<Technician> technicians) {
        if (technicians == null || technicians.isEmpty()) return List.of();
        Map<Long, Technician> byId = new LinkedHashMap<>();
        for (Technician tech : technicians) {
            if (tech == null || tech.getId() == null) continue;
            byId.putIfAbsent(tech.getId(), tech);
        }
        return List.copyOf(byId.values());
    }

    private record TimeInterval(LocalDateTime start, LocalDateTime end) {}

    private String resolveTechnicianName(Technician tech) {
        if (tech == null) return null;
        if (tech.getFullName() != null && !tech.getFullName().isBlank()) {
            return tech.getFullName();
        }
        String first = tech.getFirstName() != null ? tech.getFirstName().trim() : "";
        String last = tech.getLastName() != null ? tech.getLastName().trim() : "";
        String combined = (first + " " + last).trim();
        if (!combined.isBlank()) return combined;
        return tech.getEmail();
    }

    private long computeWorkingSeconds(WorkOrderCheckLog log) {
        if (log.getCheckInAt() == null) return 0;

        LocalDateTime end = log.getCheckOutAt() != null ? log.getCheckOutAt() : LocalDateTime.now();
        if (end.isBefore(log.getCheckInAt())) return 0;

        long totalSeconds = ChronoUnit.SECONDS.between(log.getCheckInAt(), end);
        List<WorkOrderPauseLog> pauses = resolvePauseLogs(log);
        long pausedSeconds = 0;
        for (WorkOrderPauseLog pause : pauses) {
            if (pause.getPauseAt() == null) continue;
            LocalDateTime pauseStart = pause.getPauseAt().isBefore(log.getCheckInAt()) ? log.getCheckInAt() : pause.getPauseAt();
            LocalDateTime pauseEnd = pause.getResumeAt() != null ? pause.getResumeAt() : end;
            if (pauseEnd.isBefore(pauseStart)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resumeAt cannot be before pauseAt");
            }
            if (pauseStart.isAfter(end)) continue;
            pausedSeconds += ChronoUnit.SECONDS.between(pauseStart, pauseEnd);
        }

        long workingSeconds = totalSeconds - pausedSeconds;
        return Math.max(workingSeconds, 0);
    }

    private long computeTotalWorkingSeconds(List<WorkOrderCheckLog> logs) {
        return logs.stream()
                .mapToLong(this::computeWorkingSeconds)
                .sum();
    }

    private Technician resolveTechnician(Long technicianId) {
        if (technicianId == null) return null;
        return technicianRepository.findByIdAndIsDeletedFalse(technicianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Technician not found: " + technicianId));
    }

    private TechnicianTeam resolveTeam(Long teamId) {
        if (teamId == null) return null;
        return technicianTeamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Technician team not found: " + teamId));
    }

    private boolean hasAssignmentChanged(Technician previous, Technician current) {
        if (previous == null && current == null) return false;
        if (previous == null || current == null) return true;
        return !previous.getId().equals(current.getId());
    }

    private boolean hasTeamChanged(TechnicianTeam previous, TechnicianTeam current) {
        if (previous == null && current == null) return false;
        if (previous == null || current == null) return true;
        return !previous.getId().equals(current.getId());
    }

    private void notifyAssignment(WorkOrder workOrder) {
        if (workOrder == null) return;
        notificationService.sendWorkOrderAssigned(
                workOrder,
                workOrder.getAssignedTechnician(),
                workOrder.getAssignedTeam() != null ? workOrder.getAssignedTeam().getId() : null
        );
    }

    private WorkOrderLaborEntry buildLaborEntry(WorkOrder workOrder, WorkOrderLaborEntryRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Labor entry payload is required");
        }
        BigDecimal hours = normalizeHours(request.getLaborHours(), "laborHours");
        Technician technician = resolveTechnician(request.getTechnicianId());
        BigDecimal hourlyRate = normalizeCurrency(request.getHourlyRate(), "hourlyRate");
        BigDecimal laborCost = hourlyRate != null
                ? hourlyRate.multiply(hours).setScale(2, RoundingMode.HALF_UP)
                : null;

        return WorkOrderLaborEntry.builder()
                .workOrder(workOrder)
                .technician(technician)
                .technicianNameSnapshot(technician != null ? technician.getFullName() : null)
                .laborHours(hours)
                .hourlyRate(hourlyRate)
                .laborCost(laborCost)
                .laborDate(request.getLaborDate())
                .notes(request.getNotes())
                .build();
    }

    private WorkOrderMaterialPlan buildMaterialPlan(WorkOrder workOrder, WorkOrderMaterialPlanRequest request) {
        if (request == null || request.getInventoryItemId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inventory item is required for planned material");
        }
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Planned material quantity must be >= 1");
        }

        InventoryItem item = inventoryItemRepository.findById(request.getInventoryItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Inventory item not found: " + request.getInventoryItemId()));
        if (!item.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Inventory item is inactive: " + request.getInventoryItemId());
        }

        int stock = item.getStockLevel() != null ? item.getStockLevel() : 0;
        if (stock < request.getQuantity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Insufficient stock for item %s. Requested %d, available %d"
                            .formatted(item.getItemId(), request.getQuantity(), stock));
        }

        BigDecimal unitCost = normalizeCurrency(item.getCostPerUnit(), "costPerUnit");
        BigDecimal totalCost = unitCost != null
                ? unitCost.multiply(BigDecimal.valueOf(request.getQuantity())).setScale(2, RoundingMode.HALF_UP)
                : null;

        return WorkOrderMaterialPlan.builder()
                .workOrder(workOrder)
                .inventoryItem(item)
                .quantityPlanned(request.getQuantity())
                .unitCostSnapshot(unitCost)
                .totalCostSnapshot(totalCost)
                .notes(request.getNotes())
                .build();
    }

    private WorkOrderMaterialUsage buildMaterialUsage(WorkOrder workOrder, WorkOrderMaterialUsageRequest request) {
        if (request == null || request.getInventoryItemId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inventory item is required for material usage");
        }
        if (request.getQuantityUsed() == null || request.getQuantityUsed() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantityUsed must be >= 1");
        }

        InventoryItem item = inventoryItemRepository.findById(request.getInventoryItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Inventory item not found: " + request.getInventoryItemId()));
        if (!item.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Inventory item is inactive: " + request.getInventoryItemId());
        }

        int currentStock = item.getStockLevel() != null ? item.getStockLevel() : 0;
        if (currentStock < request.getQuantityUsed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Insufficient stock for item " + item.getItemId());
        }

        item.setStockLevel(currentStock - request.getQuantityUsed());
        inventoryItemRepository.save(item);

        BigDecimal unitCost = normalizeCurrency(item.getCostPerUnit(), "costPerUnit");
        BigDecimal totalCost = unitCost != null
                ? unitCost.multiply(BigDecimal.valueOf(request.getQuantityUsed())).setScale(2, RoundingMode.HALF_UP)
                : null;

        return WorkOrderMaterialUsage.builder()
                .workOrder(workOrder)
                .inventoryItem(item)
                .quantityUsed(request.getQuantityUsed())
                .unitCostSnapshot(unitCost)
                .totalCostSnapshot(totalCost)
                .notes(request.getNotes())
                .build();
    }

    private BigDecimal normalizeHours(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be greater than zero");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeCurrency(BigDecimal value, String fieldName) {
        if (value == null) return null;
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " cannot be negative");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal addCosts(BigDecimal left, BigDecimal right) {
        if (left == null) return right;
        if (right == null) return left;
        return left.add(right);
    }

    private List<AvailabilitySlotResponse> computeAvailabilitySlots(Long technicianId,
                                                                    Long teamId,
                                                                    Technician technician,
                                                                    TechnicianTeam team,
                                                                    WorkOrderAvailabilityRequest request) {
        LocalDate fromDate = request.getFromDate() != null ? request.getFromDate() : LocalDate.now();
        LocalDate toDate = request.getToDate() != null ? request.getToDate() : fromDate.plusDays(14);
        if (toDate.isBefore(fromDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "toDate must be on or after fromDate");
        }

        int slotMinutes = request.getSlotMinutes() != null ? request.getSlotMinutes() : 60;
        if (slotMinutes <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "slotMinutes must be greater than 0");
        }

        LocalDateTime rangeStart = fromDate.atStartOfDay();
        LocalDateTime rangeEnd = toDate.plusDays(1).atStartOfDay(); // inclusive end-of-day

        List<WorkOrder> bookings = workOrderRepository.findBookingsForAssignments(
                technicianId,
                teamId,
                rangeStart,
                rangeEnd,
                Set.of(WorkOrderStatus.SCHEDULED, WorkOrderStatus.IN_PROGRESS)
        );

        List<TimeWindow> mergedBusy = mergeIntervals(
                bookings.stream()
                        .map(wo -> new TimeWindow(wo.getPlannedStartDateTime(), wo.getPlannedEndDateTime()))
                        .sorted(Comparator.comparing(TimeWindow::start))
                        .toList()
        );

        List<TimeWindow> freeWindows = computeFreeWindows(rangeStart, rangeEnd, mergedBusy);

        List<AvailabilitySlotResponse> slots = new ArrayList<>();
        for (TimeWindow window : freeWindows) {
            LocalDateTime cursor = window.start();
            while (cursor.plusMinutes(slotMinutes).isBefore(window.end()) || cursor.plusMinutes(slotMinutes).equals(window.end())) {
                LocalDateTime slotEnd = cursor.plusMinutes(slotMinutes);
                slots.add(AvailabilitySlotResponse.builder()
                        .start(cursor)
                        .end(slotEnd)
                        .technicianId(technician != null ? technician.getId() : null)
                        .technicianName(technician != null ? technician.getFullName() : null)
                        .teamId(team != null ? team.getId() : null)
                        .teamName(team != null ? team.getTeamName() : null)
                        .build());
                cursor = slotEnd;
            }
        }
        return slots;
    }

    private List<TimeWindow> mergeIntervals(List<TimeWindow> intervals) {
        if (intervals.isEmpty()) return List.of();
        List<TimeWindow> merged = new ArrayList<>();
        TimeWindow current = intervals.get(0);

        for (int i = 1; i < intervals.size(); i++) {
            TimeWindow next = intervals.get(i);
            if (!next.start().isAfter(current.end())) {
                LocalDateTime newEnd = next.end().isAfter(current.end()) ? next.end() : current.end();
                current = new TimeWindow(current.start(), newEnd);
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    private List<TimeWindow> computeFreeWindows(LocalDateTime rangeStart, LocalDateTime rangeEnd, List<TimeWindow> busy) {
        List<TimeWindow> free = new ArrayList<>();
        LocalDateTime cursor = rangeStart;
        for (TimeWindow block : busy) {
            if (cursor.isBefore(block.start())) {
                free.add(new TimeWindow(cursor, block.start()));
            }
            if (cursor.isBefore(block.end())) {
                cursor = block.end();
            }
        }
        if (cursor.isBefore(rangeEnd)) {
            free.add(new TimeWindow(cursor, rangeEnd));
        }
        return free;
    }

    private record TimeWindow(LocalDateTime start, LocalDateTime end) { }

    private String trim(String val) {
        if (val == null) return null;
        String t = val.trim();
        return t.isEmpty() ? null : t;
    }

    private WorkOrderLaborEntryResponse toLaborEntryResponse(WorkOrderLaborEntry entry) {
        Technician technician = entry.getTechnician();
        String techName = entry.getTechnicianNameSnapshot();
        if (techName == null && technician != null) {
            techName = technician.getFullName();
        }

        return WorkOrderLaborEntryResponse.builder()
                .id(entry.getId())
                .technicianId(technician != null ? technician.getId() : null)
                .technicianName(techName)
                .laborHours(entry.getLaborHours())
                .hourlyRate(entry.getHourlyRate())
                .laborCost(entry.getLaborCost())
                .laborDate(entry.getLaborDate())
                .notes(entry.getNotes())
                .build();
    }

    private WorkOrderMaterialUsageResponse toMaterialUsageResponse(WorkOrderMaterialUsage usage) {
        InventoryItem item = usage.getInventoryItem();
        return WorkOrderMaterialUsageResponse.builder()
                .id(usage.getId())
                .inventoryItemId(item != null ? item.getId() : null)
                .itemId(item != null ? item.getItemId() : null)
                .itemName(item != null ? item.getItemName() : null)
                .quantityUsed(usage.getQuantityUsed())
                .unitCostSnapshot(usage.getUnitCostSnapshot())
                .totalCostSnapshot(usage.getTotalCostSnapshot())
                .notes(usage.getNotes())
                .build();
    }

    private WorkOrderMaterialPlanResponse toMaterialPlanResponse(WorkOrderMaterialPlan plan) {
        InventoryItem item = plan.getInventoryItem();
        return WorkOrderMaterialPlanResponse.builder()
                .id(plan.getId())
                .inventoryItemId(item != null ? item.getId() : null)
                .itemId(item != null ? item.getItemId() : null)
                .itemName(item != null ? item.getItemName() : null)
                .quantityPlanned(plan.getQuantityPlanned())
                .unitCostSnapshot(plan.getUnitCostSnapshot())
                .totalCostSnapshot(plan.getTotalCostSnapshot())
                .notes(plan.getNotes())
                .build();
    }

    private WorkOrderCheckLogResponse toCheckLogResponse(WorkOrderCheckLog log) {
        List<WorkOrderPauseWindowResponse> pauses = resolvePauseLogs(log).stream()
                .map(p -> WorkOrderPauseWindowResponse.builder()
                        .pauseAt(p.getPauseAt())
                        .resumeAt(p.getResumeAt())
                        .build())
                .toList();

        return WorkOrderCheckLogResponse.builder()
                .id(log.getId())
                .technicianId(log.getTechnician() != null ? log.getTechnician().getId() : null)
                .technicianName(resolveTechnicianName(log.getTechnician()))
                .teamId(log.getTeam() != null ? log.getTeam().getId() : null)
                .teamName(log.getTeam() != null ? log.getTeam().getTeamName() : null)
                .checkInAt(log.getCheckInAt())
                .checkOutAt(log.getCheckOutAt())
                .notes(log.getNotes())
                .pauses(pauses)
                .build();
    }

    private WorkType mapMaintenanceTypeToWorkType(MaintenanceType mt) {
        if (mt == null) return WorkType.CORRECTIVE;
        return switch (mt) {
            case CORRECTIVE -> WorkType.CORRECTIVE;
            case PREVENTIVE -> WorkType.PREVENTIVE;
            case PREDICTIVE -> WorkType.PREDICTIVE;
            case EMERGENCY -> WorkType.EMERGENCY;
        };
    }

    private WorkOrderDetailsResponse toDetailsResponse(WorkOrder wo) {
        Asset asset = wo.getAsset();
        ServiceMaintenance sr = wo.getLinkedRequest();
        Technician technician = wo.getAssignedTechnician();
        TechnicianTeam team = wo.getAssignedTeam();
        WorkRequestType workRequestType = wo.getWorkRequestType();
        WorkOrderTypeTemplate woType = wo.getWorkOrderTypeTemplate();

        AssetWarrantyLifecycleDto warrantyDto = null;
        AssetWarrantyLifecycle wl = asset != null ? asset.getWarrantyLifecycle() : null;
        if (wl != null) {
            warrantyDto = new AssetWarrantyLifecycleDto();
            warrantyDto.setCommissioningDate(wl.getCommissioningDate());
            warrantyDto.setWarrantyStart(wl.getWarrantyStart());
            warrantyDto.setWarrantyEnd(wl.getWarrantyEnd());
            warrantyDto.setWarrantyProvider(wl.getWarrantyProvider());
            warrantyDto.setServiceContract(wl.getServiceContract());
            warrantyDto.setPlannedReplacementDate(wl.getPlannedReplacementDate());
            warrantyDto.setLastMaintenanceDate(wl.getLastMaintenanceDate());
            warrantyDto.setNextPlannedMaintenance(wl.getNextPlannedMaintenance());
        }

        List<WorkOrderChecklistItemResponse> checklistItems = workOrderChecklistItemRepository.findByWorkOrder_Id(wo.getId()).stream()
                .map(item -> WorkOrderChecklistItemResponse.builder()
                        .id(item.getId())
                        .itemText(item.getItemText())
                        .required(item.getRequired())
                        .completed(item.getCompleted())
                        .build())
                .toList();

        List<WorkOrderLaborEntryResponse> laborEntries = workOrderLaborEntryRepository.findByWorkOrder_Id(wo.getId()).stream()
                .map(this::toLaborEntryResponse)
                .toList();

        List<WorkOrderMaterialUsageResponse> materialUsages = workOrderMaterialUsageRepository.findByWorkOrder_Id(wo.getId()).stream()
                .map(this::toMaterialUsageResponse)
                .toList();

        List<WorkOrderMaterialPlanResponse> plannedMaterials = workOrderMaterialPlanRepository.findByWorkOrder_Id(wo.getId()).stream()
                .map(this::toMaterialPlanResponse)
                .toList();

        List<WorkOrderCheckLog> checkLogEntities = workOrderCheckLogRepository.findByWorkOrder_Id(wo.getId());
        checkLogEntities.sort(Comparator.comparing(WorkOrderCheckLog::getCheckInAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(WorkOrderCheckLog::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        long actualWorkingSeconds = computeTotalWorkingSeconds(checkLogEntities);
        BigDecimal actualWorkingHours = BigDecimal.valueOf(actualWorkingSeconds)
                .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);

        List<WorkOrderCheckLogResponse> checkLogs = checkLogEntities.stream()
                .map(this::toCheckLogResponse)
                .toList();

        Long emergencyIncidentId = emergencyIncidentRepository.findByWorkOrder_Id(wo.getId())
                .map(em -> em.getId())
                .orElse(null);

        return WorkOrderDetailsResponse.builder()
                .id(wo.getId())
                .workOrderNumber(wo.getWoNumber())
                .workOrderId(wo.getWorkOrderId())
                .linkedServiceRequestDbId(sr != null ? sr.getId() : null)
                .linkedServiceRequestId(sr != null ? sr.getRequestId() : null)
                .pmPlanId(wo.getPmPlan() != null ? wo.getPmPlan().getId() : null)
                .pmPlanCode(wo.getPmPlan() != null ? wo.getPmPlan().getPlanCode() : null)
                .pmDueDate(wo.getPmDueDate())
                .emergencyIncidentId(emergencyIncidentId)
                .assetDbId(asset != null ? asset.getId() : null)
                .assetId(asset != null ? asset.getAssetId() : null)
                .assetName(asset != null ? asset.getAssetName() : null)
                .warrantyLifecycle(warrantyDto)
                .location(wo.getLocation())
                .workType(wo.getWorkType())
                .priority(wo.getPriority())
                .workRequestTypeId(workRequestType != null ? workRequestType.getId() : null)
                .workRequestTypeCode(workRequestType != null ? workRequestType.getCode() : null)
                .workRequestTypeDescription(workRequestType != null ? workRequestType.getDescription() : null)
                .woTitle(wo.getWoTitle())
                .descriptionScope(wo.getDescriptionScope())
                .planner(wo.getPlanner())
                .assignedTechnicianId(technician != null ? technician.getId() : null)
                .assignedTechnicianName(technician != null ? technician.getFullName() : null)
                .assignedTeamId(team != null ? team.getId() : null)
                .assignedTeamName(team != null ? team.getTeamName() : null)
                .workOrderTypeId(woType != null ? woType.getId() : null)
                .workOrderTypeName(woType != null ? woType.getWorkOrderType() : null)
                .glAccount(wo.getGlAccount())
                .utilityAccount(wo.getUtilityAccount())
                .laborGlAccount(wo.getLaborGlAccount())
                .laborUtilityAccount(wo.getLaborUtilityAccount())
                .inventoryGlAccount(wo.getInventoryGlAccount())
                .inventoryUtilityAccount(wo.getInventoryUtilityAccount())
                .teamMembers(team != null ? toTeamMemberResponses(team) : null)
                .plannedStartDateTime(wo.getPlannedStartDateTime())
                .plannedEndDateTime(wo.getPlannedEndDateTime())
                .actualStartDateTime(wo.getActualStartDateTime())
                .actualEndDateTime(wo.getActualEndDateTime())
                .targetCompletionDate(wo.getTargetCompletionDate())
                .estimatedLaborHours(wo.getEstimatedLaborHours())
                .estimatedMaterialCost(wo.getEstimatedMaterialCost())
                .estimatedTotalCost(wo.getEstimatedTotalCost())
                .actualLaborHours(wo.getActualLaborHours())
                .actualWorkingHours(actualWorkingHours)
                .actualLaborCost(wo.getActualLaborCost())
                .actualMaterialCost(wo.getActualMaterialCost())
                .actualTotalCost(wo.getActualTotalCost())
                .completionNotes(wo.getCompletionNotes())
                .failureDescription(wo.getFailureDescription())
                .failureCause(wo.getFailureCause())
                .remedyAction(wo.getRemedyAction())
                .downtimeStart(wo.getDowntimeStart())
                .downtimeEnd(wo.getDowntimeEnd())
                .reporter(wo.getReporter())
                .beforePhotoUrl(wo.getBeforePhotoUrl())
                .afterPhotoUrl(wo.getAfterPhotoUrl())
                .supervisorNotes(wo.getSupervisorNotes())
                .approvalNotes(wo.getApprovalNotes())
                .rejectionReason(wo.getRejectionReason())
                .precheckNotes(wo.getPrecheckNotes())
                .approvedBy(wo.getApprovedBy())
                .approvedAt(wo.getApprovedAt())
                .rejectedBy(wo.getRejectedBy())
                .rejectedAt(wo.getRejectedAt())
                .status(wo.getStatus())
                .source(wo.getSource())
                .plannedMaterials(plannedMaterials)
                .checkLogs(checkLogs)
                .laborEntries(laborEntries)
                .checklistItems(checklistItems)
                .materialUsages(materialUsages)
                .createdAt(wo.getCreatedAt())
                .updatedAt(wo.getUpdatedAt())
                .build();
    }
    private PriorityLevel mapRequestPriorityToPriorityLevel(RequestPriority p) {
    if (p == null) return null;

    return switch (p) {
        case LOW -> PriorityLevel.LOW;
        case MEDIUM -> PriorityLevel.MEDIUM;
        case HIGH -> PriorityLevel.HIGH;
        case CRITICAL -> PriorityLevel.CRITICAL;
    };
}

}
