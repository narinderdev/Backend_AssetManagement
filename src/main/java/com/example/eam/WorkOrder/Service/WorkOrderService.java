package com.example.eam.WorkOrder.Service;


import com.example.eam.Asset.Entity.Asset;
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
import com.example.eam.WorkOrder.Dto.*;
import com.example.eam.WorkOrder.Entity.WorkOrder;
import com.example.eam.WorkOrder.Entity.WorkOrderLaborEntry;
import com.example.eam.WorkOrder.Entity.WorkOrderMaterialPlan;
import com.example.eam.WorkOrder.Entity.WorkOrderMaterialUsage;
import com.example.eam.WorkOrder.Entity.WorkOrderCheckLog;
import com.example.eam.WorkOrder.Repository.WorkOrderLaborEntryRepository;
import com.example.eam.WorkOrder.Repository.WorkOrderMaterialPlanRepository;
import com.example.eam.WorkOrder.Repository.WorkOrderMaterialUsageRepository;
import com.example.eam.WorkOrder.Repository.WorkOrderRepository;
import com.example.eam.WorkOrder.Repository.WorkOrderCheckLogRepository;
import com.example.eam.WorkOrder.Repository.WorkOrderChecklistItemRepository;
import com.example.eam.Maintenance.Emergency.Repository.EmergencyIncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import java.util.List;
import java.util.Map;
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
    private final InventoryItemRepository inventoryItemRepository;
    private final WorkOrderLaborEntryRepository workOrderLaborEntryRepository;
    private final WorkOrderMaterialUsageRepository workOrderMaterialUsageRepository;
    private final WorkOrderMaterialPlanRepository workOrderMaterialPlanRepository;
    private final WorkOrderCheckLogRepository workOrderCheckLogRepository;
    private final EmergencyIncidentRepository emergencyIncidentRepository;

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

        WorkOrder wo = WorkOrder.builder()
                .workOrderId(generateUniqueWorkOrderId())
                .linkedRequest(null)
                .asset(asset)
                .location(location)
                .workType(request.getWorkType())
                .priority(request.getPriority())
                .woTitle(request.getWoTitle())
                .descriptionScope(request.getDescriptionScope())
                .planner(null)
                .assignedTechnician(null)
                .assignedTeam(null)
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

        String generatedWoId = generateUniqueWorkOrderId();
        log.info("Generated Work Order ID: {}", generatedWoId);

        WorkOrder wo = WorkOrder.builder()
        .workOrderId(generatedWoId)
        .linkedRequest(sr)
        .asset(asset)
        .location(location)
        .workType(workType != null ? workType : WorkType.CORRECTIVE)
        .priority(priority != null ? priority : PriorityLevel.MEDIUM)
        .woTitle(!isBlank(woTitle) ? woTitle : "Work Order from Service Request")
        .descriptionScope(desc)
        .planner(null)
        .assignedTechnician(null)
        .assignedTeam(null)
        .plannedStartDateTime(null)
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

    // ---------------- PATCH UPDATE ----------------

    @Transactional
    public WorkOrderDetailsResponse patchWorkOrder(Long id, WorkOrderPatchRequest request) {
        WorkOrder wo = getWorkOrderOrThrow(id);
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
        return toDetailsResponse(saved);
    }

    @Transactional
    public WorkOrderDetailsResponse markInProgress(Long id, WorkOrderInProgressRequest request) {
        WorkOrder wo = getWorkOrderOrThrow(id);
        if (wo.getStatus() != WorkOrderStatus.SCHEDULED && wo.getStatus() != WorkOrderStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Work Order must be SCHEDULED or IN_PROGRESS to log check-in/out");
        }

        Technician technician = request.getTechnicianId() != null ? resolveTechnician(request.getTechnicianId()) : null;
        TechnicianTeam team = request.getTeamId() != null ? resolveTeam(request.getTeamId()) : null;

        LocalDateTime checkIn = request.getCheckInAt() != null
                ? request.getCheckInAt()
                : LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        WorkOrderCheckLog log = WorkOrderCheckLog.builder()
                .workOrder(wo)
                .technician(technician)
                .team(team)
                .checkInAt(checkIn)
                .checkOutAt(request.getCheckOutAt())
                .notes(trim(request.getNotes()))
                .build();
        workOrderCheckLogRepository.save(log);

        // Set actual start/end to reflect the earliest/latest entries
        if (wo.getActualStartDateTime() == null || checkIn.isBefore(wo.getActualStartDateTime())) {
            wo.setActualStartDateTime(checkIn);
        }
        if (request.getCheckOutAt() != null) {
            LocalDateTime co = request.getCheckOutAt();
            if (wo.getActualEndDateTime() == null || co.isAfter(wo.getActualEndDateTime())) {
                wo.setActualEndDateTime(co);
            }
        }

        if (wo.getStatus() == WorkOrderStatus.SCHEDULED) {
            validateStatusTransition(wo.getStatus(), WorkOrderStatus.IN_PROGRESS);
            handleStatusSideEffects(wo, WorkOrderStatus.IN_PROGRESS);
            wo.setStatus(WorkOrderStatus.IN_PROGRESS);
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

        wo.setActualLaborHours(totalLaborHours.compareTo(BigDecimal.ZERO) > 0 ? totalLaborHours : null);
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
        Page<WorkOrder> page = workOrderRepository.findByDeletedFalse(pageable);
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
            String candidate = String.format("WO-%s-%04d", datePart, rand);

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

    private Technician resolveTechnician(Long technicianId) {
        if (technicianId == null) return null;
        return technicianRepository.findById(technicianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Technician not found: " + technicianId));
    }

    private TechnicianTeam resolveTeam(Long teamId) {
        if (teamId == null) return null;
        return technicianTeamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Technician team not found: " + teamId));
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
        return WorkOrderCheckLogResponse.builder()
                .id(log.getId())
                .technicianId(log.getTechnician() != null ? log.getTechnician().getId() : null)
                .teamId(log.getTeam() != null ? log.getTeam().getId() : null)
                .checkInAt(log.getCheckInAt())
                .checkOutAt(log.getCheckOutAt())
                .notes(log.getNotes())
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

        List<WorkOrderCheckLogResponse> checkLogs = workOrderCheckLogRepository.findByWorkOrder_Id(wo.getId()).stream()
                .map(this::toCheckLogResponse)
                .toList();

        Long emergencyIncidentId = emergencyIncidentRepository.findByWorkOrder_Id(wo.getId())
                .map(em -> em.getId())
                .orElse(null);

        return WorkOrderDetailsResponse.builder()
                .id(wo.getId())
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
                .location(wo.getLocation())
                .workType(wo.getWorkType())
                .priority(wo.getPriority())
                .woTitle(wo.getWoTitle())
                .descriptionScope(wo.getDescriptionScope())
                .planner(wo.getPlanner())
                .assignedTechnicianId(technician != null ? technician.getId() : null)
                .assignedTechnicianName(technician != null ? technician.getFullName() : null)
                .assignedTeamId(team != null ? team.getId() : null)
                .assignedTeamName(team != null ? team.getTeamName() : null)
                .plannedStartDateTime(wo.getPlannedStartDateTime())
                .plannedEndDateTime(wo.getPlannedEndDateTime())
                .actualStartDateTime(wo.getActualStartDateTime())
                .actualEndDateTime(wo.getActualEndDateTime())
                .targetCompletionDate(wo.getTargetCompletionDate())
                .estimatedLaborHours(wo.getEstimatedLaborHours())
                .estimatedMaterialCost(wo.getEstimatedMaterialCost())
                .estimatedTotalCost(wo.getEstimatedTotalCost())
                .actualLaborHours(wo.getActualLaborHours())
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
