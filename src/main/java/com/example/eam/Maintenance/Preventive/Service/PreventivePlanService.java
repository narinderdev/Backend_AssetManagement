package com.example.eam.Maintenance.Preventive.Service;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Asset.Entity.AssetLocation;
import com.example.eam.Asset.Repository.AssetLocationRepository;
import com.example.eam.Asset.Repository.AssetRepository;
import com.example.eam.Enum.*;
import com.example.eam.Maintenance.Preventive.Dto.PreventivePlanCreateRequest;
import com.example.eam.Maintenance.Preventive.Dto.PreventivePlanPatchRequest;
import com.example.eam.Maintenance.Preventive.Dto.PreventivePlanResponse;
import com.example.eam.Maintenance.Preventive.Entity.PreventivePlan;
import com.example.eam.Maintenance.Preventive.Repository.PreventivePlanRepository;
import com.example.eam.Asset.Service.AssetTypeService;
import com.example.eam.WorkOrder.Entity.WorkOrder;
import com.example.eam.WorkOrder.Repository.WorkOrderRepository;
import com.example.eam.WorkOrder.Service.WoNumberPoolService;
import com.example.eam.WorkRequestType.Entity.WorkRequestType;
import com.example.eam.WorkRequestType.Service.WorkRequestTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class PreventivePlanService {

    private final PreventivePlanRepository planRepository;
    private final AssetRepository assetRepository;
    private final AssetLocationRepository assetLocationRepository;
    private final AssetTypeService assetTypeService;
    private final WorkOrderRepository workOrderRepository;
    private final WorkRequestTypeService workRequestTypeService;
    private final WoNumberPoolService woNumberPoolService;

    // ---------- CREATE ----------

    @Transactional
    public PreventivePlanResponse create(@Valid PreventivePlanCreateRequest req) {
        PreventiveApplyTarget target = req.getApplyTo();
        if (target == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "applyTo is required");
        }

        java.util.List<Asset> targetAssets = switch (target) {
            case ASSET -> java.util.List.of(resolveAssetRequired(req.getAssetId()));
            case ASSET_TYPE -> resolveAssetsByType(req.getAssetTypeId());
        };

        if (targetAssets.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No assets found for the requested target");
        }

        validateSchedule(req.getScheduleType(), req.getIntervalUnit(), req.getIntervalValue(),
                req.getMeterType(), req.getMeterIntervalValue(), req.getStartDate());

        PreventivePlanResponse firstResponse = null;
        for (Asset asset : targetAssets) {
            String location = resolveLocation(asset, req.getLocation());

            PreventivePlan plan = PreventivePlan.builder()
                    .planCode(generateUniquePlanCode())
                    .title(req.getTitle())
                    .asset(asset)
                    .location(location)
                    .workType(req.getWorkType() != null ? req.getWorkType() : WorkType.PREVENTIVE)
                    .priority(req.getPriority() != null ? req.getPriority() : PriorityLevel.MEDIUM)
                    .scheduleType(req.getScheduleType())
                    .leadTimeDays(req.getLeadTimeDays())
                    .startDate(req.getStartDate())
                    .intervalUnit(req.getIntervalUnit())
                    .intervalValue(req.getIntervalValue())
                    .meterType(req.getMeterType())
                    .meterIntervalValue(req.getMeterIntervalValue())
                    .currentMeterReading(req.getCurrentMeterReading())
                    .nextDueDate(req.getScheduleType() == PreventiveScheduleType.TIME_BASED ? req.getStartDate() : null)
                    .nextDueMeter(req.getScheduleType() == PreventiveScheduleType.USAGE_BASED && req.getMeterIntervalValue() != null && req.getCurrentMeterReading() != null
                            ? req.getCurrentMeterReading() + req.getMeterIntervalValue() : null)
                    .lastGeneratedDueDate(null)
                    .active(true)
                    .deleted(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            PreventivePlan saved = planRepository.save(plan);
            if (firstResponse == null) {
                firstResponse = mapToResponse(saved);
            }
        }

        return firstResponse;
    }

    // ---------- PATCH ----------

    @Transactional
    public PreventivePlanResponse patch(Long id, PreventivePlanPatchRequest req) {
        PreventivePlan plan = getPlanOrThrow(id);
        Asset asset = req.getAssetId() != null ? resolveAsset(req.getAssetId()) : plan.getAsset();
        String location = req.getLocation() != null ? resolveLocation(asset, req.getLocation()) : plan.getLocation();

        if (req.getScheduleType() != null || req.getIntervalUnit() != null || req.getIntervalValue() != null
                || req.getMeterType() != null || req.getMeterIntervalValue() != null || req.getStartDate() != null) {
            validateSchedule(
                    req.getScheduleType() != null ? req.getScheduleType() : plan.getScheduleType(),
                    req.getIntervalUnit() != null ? req.getIntervalUnit() : plan.getIntervalUnit(),
                    req.getIntervalValue() != null ? req.getIntervalValue() : plan.getIntervalValue(),
                    req.getMeterType() != null ? req.getMeterType() : plan.getMeterType(),
                    req.getMeterIntervalValue() != null ? req.getMeterIntervalValue() : plan.getMeterIntervalValue(),
                    req.getStartDate() != null ? req.getStartDate() : plan.getStartDate()
            );
        }

        updateIfNotNull(asset, plan::setAsset);
        if (req.getLocation() != null) plan.setLocation(location);
        updateIfNotBlank(req.getTitle(), plan::setTitle);
        updateIfNotNull(req.getWorkType(), plan::setWorkType);
        updateIfNotNull(req.getPriority(), plan::setPriority);
        updateIfNotNull(req.getScheduleType(), plan::setScheduleType);
        updateIfNotNull(req.getLeadTimeDays(), plan::setLeadTimeDays);
        updateIfNotNull(req.getStartDate(), plan::setStartDate);
        updateIfNotNull(req.getIntervalUnit(), plan::setIntervalUnit);
        updateIfNotNull(req.getIntervalValue(), plan::setIntervalValue);
        updateIfNotNull(req.getMeterType(), plan::setMeterType);
        updateIfNotNull(req.getMeterIntervalValue(), plan::setMeterIntervalValue);
        updateIfNotNull(req.getCurrentMeterReading(), plan::setCurrentMeterReading);
        updateIfNotNull(req.getActive(), plan::setActive);

        // recalc schedule
        if (plan.getScheduleType() == PreventiveScheduleType.TIME_BASED) {
            plan.setNextDueDate(plan.getLastGeneratedDueDate() != null
                    ? addInterval(plan.getLastGeneratedDueDate(), plan.getIntervalValue(), plan.getIntervalUnit())
                    : plan.getStartDate());
            plan.setNextDueMeter(null);
        } else {
            Integer base = plan.getCurrentMeterReading() != null ? plan.getCurrentMeterReading() : 0;
            Integer interval = plan.getMeterIntervalValue();
            plan.setNextDueMeter(interval != null ? base + interval : null);
            plan.setNextDueDate(null);
        }
        plan.setUpdatedAt(LocalDateTime.now());

        planRepository.save(plan);

        return mapToResponse(plan);
    }

    // ---------- READ ----------

    @Transactional(readOnly = true)
    public PreventivePlanResponse get(Long id) {
        PreventivePlan plan = getPlanOrThrow(id);
        return mapToResponse(plan);
    }

    @Transactional(readOnly = true)
    public Page<PreventivePlanResponse> list(Pageable pageable) {
        return planRepository.findByDeletedFalse(pageable)
                .map(this::mapToResponse);
    }

    // ---------- DELETE ----------

    @Transactional
    public void delete(Long id) {
        PreventivePlan plan = getPlanOrThrow(id);
        plan.setDeleted(true);
        plan.setActive(false);
        plan.setUpdatedAt(LocalDateTime.now());
        planRepository.save(plan);
    }

    // ---------- GENERATION ----------

    @Transactional
    public void generateDueWorkOrders() {
        LocalDate today = LocalDate.now();

        planRepository.findAll().stream()
                .filter(p -> Boolean.FALSE.equals(p.getDeleted()))
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .filter(p -> p.getScheduleType() == PreventiveScheduleType.TIME_BASED)
                .forEach(plan -> {
                    LocalDate due = plan.getNextDueDate();
                    if (due == null) return;
                    int lead = plan.getLeadTimeDays() != null ? plan.getLeadTimeDays() : 0;
                    LocalDate generateOnOrAfter = due.minusDays(Math.max(0, lead));
                    if (!today.isBefore(generateOnOrAfter)) {
                        generateWorkOrder(plan, due);
                        plan.setLastGeneratedDueDate(due);
                        plan.setNextDueDate(addInterval(due, plan.getIntervalValue(), plan.getIntervalUnit()));
                        plan.setUpdatedAt(LocalDateTime.now());
                        planRepository.save(plan);
                    }
                });
    }

    // ---------- Helpers ----------

    private void generateWorkOrder(PreventivePlan plan, LocalDate dueDate) {
        if (workOrderRepository.existsByPmPlan_IdAndPmDueDate(plan.getId(), dueDate)) {
            return;
        }

        WorkRequestType workRequestType = workRequestTypeService.getOrCreateDefaultExpenseType();

        WorkOrder wo = WorkOrder.builder()
                .workOrderId(generateUniqueWorkOrderId())
                .pmPlan(plan)
                .pmDueDate(dueDate)
                .asset(plan.getAsset())
                .workRequestType(workRequestType)
                .location(plan.getLocation() != null ? plan.getLocation() : resolveLocation(plan.getAsset(), null))
                .workType(plan.getWorkType() != null ? plan.getWorkType() : WorkType.PREVENTIVE)
                .priority(plan.getPriority() != null ? plan.getPriority() : PriorityLevel.MEDIUM)
                .woTitle("PM: " + plan.getTitle())
                .descriptionScope("Preventive maintenance plan " + plan.getPlanCode())
                .planner(null)
                .assignedTechnician(null)
                .assignedTeam(null)
                .plannedStartDateTime(null)
                .plannedEndDateTime(null)
                .targetCompletionDate(dueDate)
                .estimatedLaborHours(null)
                .estimatedMaterialCost(null)
                .estimatedTotalCost(null)
                .status(WorkOrderStatus.NEW)
                .source(WorkOrderSource.PM)
                .deleted(false)
                .build();

        WorkOrder saved = workOrderRepository.save(wo);
        String woNumber = woNumberPoolService.allocateWoNumber(saved.getId());
        saved.setWoNumber(woNumber);
        workOrderRepository.save(saved);
    }

    private PreventivePlan getPlanOrThrow(Long id) {
        return planRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Preventive plan not found"));
    }

    private Asset resolveAssetRequired(Long assetId) {
        if (assetId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "assetId is required when applyTo is ASSET");
        }
        return resolveAsset(assetId);
    }

    private java.util.List<Asset> resolveAssetsByType(Long assetTypeId) {
        if (assetTypeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "assetTypeId is required when applyTo is ASSET_TYPE");
        }
        assetTypeService.getActiveAssetTypeOrThrow(assetTypeId);
        return assetRepository.findByAssetTypeRef_Id(assetTypeId);
    }

    private Asset resolveAsset(Long assetId) {
        if (assetId == null) return null;
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset not found"));
    }

    private String resolveLocation(Asset asset, String provided) {
        if (provided != null && !provided.trim().isEmpty()) return provided.trim();
        if (asset == null) return null;
        return assetLocationRepository.findByAsset_Id(asset.getId())
                .map(AssetLocation::getLocation)
                .orElse(null);
    }

    private void validateSchedule(PreventiveScheduleType type,
                                  TimeFrequencyUnit intervalUnit,
                                  Integer intervalValue,
                                  MeterType meterType,
                                  Integer meterIntervalValue,
                                  LocalDate startDate) {
        if (type == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "scheduleType is required");
        if (startDate == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate is required");

        if (type == PreventiveScheduleType.TIME_BASED) {
            if (intervalUnit == null || intervalValue == null || intervalValue < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "intervalUnit and intervalValue are required for time-based plans");
            }
        } else {
            if (meterType == null || meterIntervalValue == null || meterIntervalValue < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "meterType and meterIntervalValue are required for usage-based plans");
            }
        }
    }

    private LocalDate addInterval(LocalDate from, Integer value, TimeFrequencyUnit unit) {
        if (from == null || value == null || unit == null) return null;
        return switch (unit) {
            case DAYS -> from.plusDays(value);
            case WEEKS -> from.plusWeeks(value);
            case MONTHS -> from.plusMonths(value);
            case YEARS -> from.plusYears(value);
        };
    }

    private String generateUniquePlanCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int i = 0; i < 30; i++) {
            int rand = ThreadLocalRandom.current().nextInt(0, 10000);
            String candidate = String.format("PM-%s-%04d", datePart, rand);
            if (!planRepository.existsByPlanCode(candidate)) {
                return candidate;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate unique PM plan code");
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

    private <T> void updateIfNotNull(T value, Consumer<T> setter) {
        if (value != null) setter.accept(value);
    }

    private void updateIfNotBlank(String value, Consumer<String> setter) {
        if (value != null && !value.trim().isEmpty()) setter.accept(value.trim());
    }

    private PreventivePlanResponse mapToResponse(PreventivePlan plan) {
        Asset asset = plan.getAsset();
        return PreventivePlanResponse.builder()
                .id(plan.getId())
                .planCode(plan.getPlanCode())
                .title(plan.getTitle())
                .assetId(asset != null ? asset.getId() : null)
                .assetCode(asset != null ? asset.getAssetId() : null)
                .assetName(asset != null ? asset.getAssetName() : null)
                .assetTypeId(asset != null && asset.getAssetTypeRef() != null ? asset.getAssetTypeRef().getId() : null)
                .assetTypeCode(asset != null && asset.getAssetTypeRef() != null ? asset.getAssetTypeRef().getCode() : null)
                .assetTypeName(asset != null && asset.getAssetTypeRef() != null ? asset.getAssetTypeRef().getName() : null)
                .location(plan.getLocation())
                .workType(plan.getWorkType())
                .priority(plan.getPriority())
                .scheduleType(plan.getScheduleType())
                .leadTimeDays(plan.getLeadTimeDays())
                .startDate(plan.getStartDate())
                .intervalUnit(plan.getIntervalUnit())
                .intervalValue(plan.getIntervalValue())
                .meterType(plan.getMeterType())
                .meterIntervalValue(plan.getMeterIntervalValue())
                .currentMeterReading(plan.getCurrentMeterReading())
                .nextDueDate(plan.getNextDueDate())
                .nextDueMeter(plan.getNextDueMeter())
                .lastGeneratedDueDate(plan.getLastGeneratedDueDate())
                .active(plan.getActive())
                .build();
    }
}
