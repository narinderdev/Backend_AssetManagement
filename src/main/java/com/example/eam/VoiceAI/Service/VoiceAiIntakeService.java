package com.example.eam.VoiceAI.Service;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Asset.Repository.AssetRepository;
import com.example.eam.Common.CompanyContextHolder;
import com.example.eam.Enum.MaintenanceType;
import com.example.eam.Enum.RequestPriority;
import com.example.eam.Enum.ServiceRequestStatus;
import com.example.eam.Enum.TechnicianStatus;
import com.example.eam.Enum.WorkOrderStatus;
import com.example.eam.ServiceMaintenance.Dto.ServiceRequestApproveDto;
import com.example.eam.ServiceMaintenance.Dto.ServiceRequestCreateDto;
import com.example.eam.ServiceMaintenance.Dto.ServiceRequestResponse;
import com.example.eam.ServiceMaintenance.Dto.ServiceRequestUpdateDto;
import com.example.eam.ServiceMaintenance.Entity.ServiceMaintenance;
import com.example.eam.ServiceMaintenance.Repository.ServiceMaintenanceRepository;
import com.example.eam.ServiceMaintenance.Service.ServiceMaintenanceService;
import com.example.eam.Technician.Entity.Technician;
import com.example.eam.Technician.Repository.TechnicianRepository;
import com.example.eam.VoiceAI.Dto.VoiceInquiryStatusResponse;
import com.example.eam.VoiceAI.Dto.VoiceIntakeRequest;
import com.example.eam.VoiceAI.Dto.VoiceIntakeResponse;
import com.example.eam.VoiceAI.Entity.VoiceAiIntake;
import com.example.eam.VoiceAI.Enum.VoiceCallIntent;
import com.example.eam.VoiceAI.Enum.VoiceIntakeOutcome;
import com.example.eam.VoiceAI.Repository.VoiceAiIntakeRepository;
import com.example.eam.WorkOrder.Dto.AvailabilitySlotResponse;
import com.example.eam.WorkOrder.Dto.WorkOrderApproveRequest;
import com.example.eam.WorkOrder.Dto.WorkOrderAvailabilityRequest;
import com.example.eam.WorkOrder.Dto.WorkOrderDetailsResponse;
import com.example.eam.WorkOrder.Dto.WorkOrderScheduleRequest;
import com.example.eam.WorkOrder.Entity.WorkOrder;
import com.example.eam.WorkOrder.Repository.WorkOrderRepository;
import com.example.eam.WorkOrder.Service.WorkOrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VoiceAiIntakeService {

    private static final String SYSTEM_ACTOR = "voice-ai-system";
    private static final int DEFAULT_EXPECTED_DURATION_HOURS = 2;
    private static final int MAX_SR_DESCRIPTION = 2000;
    private static final Set<WorkOrderStatus> BOOKING_CONFLICT_STATUSES = Set.of(
            WorkOrderStatus.SCHEDULED,
            WorkOrderStatus.ON_THE_WAY,
            WorkOrderStatus.ARRIVED,
            WorkOrderStatus.IN_PROGRESS
    );
    private static final Set<String> EMERGENCY_HINTS = Set.of(
            "fire", "smoke", "sparking", "flood", "gas leak", "blast", "urgent", "emergency"
    );
    private static final Set<RequestPriority> ALLOWED_URGENCIES = EnumSet.of(
            RequestPriority.LOW,
            RequestPriority.MEDIUM,
            RequestPriority.HIGH,
            RequestPriority.CRITICAL
    );

    private final VoiceAiIntakeRepository voiceAiIntakeRepository;
    private final ServiceMaintenanceService serviceMaintenanceService;
    private final ServiceMaintenanceRepository serviceMaintenanceRepository;
    private final WorkOrderService workOrderService;
    private final WorkOrderRepository workOrderRepository;
    private final AssetRepository assetRepository;
    private final TechnicianRepository technicianRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public VoiceIntakeResponse processIntake(VoiceIntakeRequest request) {
        Long companyId = requireCompanyId();
        StructuredInput structured = structureInput(request, companyId);

        ProcessingResult result = switch (structured.intent()) {
            case STATUS_INQUIRY -> handleStatusInquiry(structured, companyId);
            case GENERAL_QUESTION -> handleGeneralQuestion(structured);
            case NEW_SERVICE_REQUEST, EMERGENCY -> handleServiceRequestFlow(structured, companyId);
        };

        VoiceAiIntake intake = persistIntake(companyId, structured, result);
        return VoiceIntakeResponse.builder()
                .voiceIntakeId(intake.getId())
                .intent(structured.intent())
                .outcome(result.outcome())
                .serviceRequest(result.serviceRequest())
                .workOrder(result.workOrder())
                .inquiryStatus(result.inquiryStatus())
                .escalationRequired(result.escalationRequired())
                .escalationReason(result.escalationReason())
                .assignedTechnician(result.assignedTechnicianName())
                .expectedResponseAt(result.expectedResponseAt())
                .customerConfirmationMessage(result.customerConfirmationMessage())
                .build();
    }

    private ProcessingResult handleGeneralQuestion(StructuredInput structured) {
        String reason = "General question or unclear intent; routed to live representative with context";
        String message = "Your request is forwarded to a live representative. Reference: " + buildExternalRef(structured);
        return ProcessingResult.builder()
                .outcome(VoiceIntakeOutcome.ESCALATED_TO_LIVE_AGENT)
                .escalationRequired(true)
                .escalationReason(reason)
                .customerConfirmationMessage(message)
                .build();
    }

    private ProcessingResult handleStatusInquiry(StructuredInput structured, Long companyId) {
        ServiceMaintenance serviceRequest = findServiceRequestForInquiry(structured, companyId);
        WorkOrder workOrder = findWorkOrderForInquiry(structured, companyId, serviceRequest);

        if (serviceRequest == null && workOrder == null) {
            String reason = "Status inquiry reference not found";
            return ProcessingResult.builder()
                    .outcome(VoiceIntakeOutcome.ESCALATED_TO_LIVE_AGENT)
                    .escalationRequired(true)
                    .escalationReason(reason)
                    .customerConfirmationMessage("We could not locate that request. A live representative will assist you.")
                    .build();
        }

        WorkOrderDetailsResponse workOrderDetails = workOrder != null
                ? workOrderService.getWorkOrderDetails(workOrder.getId())
                : null;

        VoiceInquiryStatusResponse inquiryStatus = VoiceInquiryStatusResponse.builder()
                .serviceRequestId(serviceRequest != null ? serviceRequest.getRequestId() : null)
                .serviceRequestStatus(serviceRequest != null ? serviceRequest.getStatus() : null)
                .workOrderId(workOrderDetails != null ? workOrderDetails.getWorkOrderId() : null)
                .workOrderNumber(workOrderDetails != null ? workOrderDetails.getWorkOrderNumber() : null)
                .workOrderStatus(workOrderDetails != null ? workOrderDetails.getStatus() : null)
                .assignedTechnician(workOrderDetails != null ? workOrderDetails.getAssignedTechnicianName() : null)
                .expectedResponseAt(workOrderDetails != null ? workOrderDetails.getPlannedStartDateTime() : null)
                .build();
        ServiceRequestResponse serviceRequestResponse = serviceRequest != null
                ? ServiceRequestResponse.builder()
                .id(serviceRequest.getId())
                .requestId(serviceRequest.getRequestId())
                .status(serviceRequest.getStatus())
                .build()
                : null;

        String confirmation = buildStatusInquiryMessage(inquiryStatus);

        return ProcessingResult.builder()
                .outcome(VoiceIntakeOutcome.STATUS_SHARED)
                .serviceRequest(serviceRequestResponse)
                .inquiryStatus(inquiryStatus)
                .workOrder(workOrderDetails)
                .escalationRequired(false)
                .customerConfirmationMessage(confirmation)
                .build();
    }

    private ProcessingResult handleServiceRequestFlow(StructuredInput structured, Long companyId) {
        validateServiceRequestPayload(structured);

        ServiceRequestCreateDto createDto = buildServiceRequestCreateDto(structured);
        ServiceRequestResponse created = serviceMaintenanceService.create(createDto);

        boolean highOrCritical = structured.priority() == RequestPriority.HIGH
                || structured.priority() == RequestPriority.CRITICAL;
        boolean escalationRequired = highOrCritical;
        String escalationReason = escalationRequired
                ? "Urgent/emergency request flagged for supervisor monitoring"
                : null;

        if (structured.unclearIntent()) {
            ServiceRequestUpdateDto updateDto = new ServiceRequestUpdateDto();
            updateDto.setStatus(ServiceRequestStatus.UNDER_REVIEW);
            ServiceRequestResponse underReview = serviceMaintenanceService.update(created.getId(), updateDto);
            return ProcessingResult.builder()
                    .outcome(VoiceIntakeOutcome.SERVICE_REQUEST_CREATED)
                    .serviceRequest(underReview)
                    .escalationRequired(true)
                    .escalationReason("Voice input was ambiguous and moved to UNDER_REVIEW")
                    .customerConfirmationMessage("Service request " + underReview.getRequestId() + " created and queued for live review.")
                    .build();
        }

        if (!highOrCritical) {
            return ProcessingResult.builder()
                    .outcome(VoiceIntakeOutcome.SERVICE_REQUEST_CREATED)
                    .serviceRequest(created)
                    .escalationRequired(false)
                    .customerConfirmationMessage("Service request " + created.getRequestId() + " created.")
                    .build();
        }

        ServiceRequestApproveDto srApprove = ServiceRequestApproveDto.builder()
                .approvedBy(SYSTEM_ACTOR)
                .build();
        ServiceRequestResponse approvedRequest = serviceMaintenanceService.approve(created.getId(), srApprove);

        WorkOrderDetailsResponse convertedWo = workOrderService.convertServiceRequestToWorkOrder(created.getId());

        WorkOrderApproveRequest woApprove = new WorkOrderApproveRequest();
        woApprove.setApprovedBy(SYSTEM_ACTOR);
        woApprove.setApprovalNotes("Auto-approved from voice AI intake");
        WorkOrderDetailsResponse approvedWo = workOrderService.approveWorkOrder(convertedWo.getId(), woApprove);

        AssignmentDecision assignment = chooseAssignment(structured, companyId);
        WorkOrderDetailsResponse finalWorkOrder = approvedWo;

        if (assignment != null && assignment.hasAssignment()) {
            ScheduleWindow window = assignment.window();
            if (window == null) {
                window = fallbackScheduleWindow(structured);
            }
            WorkOrderScheduleRequest scheduleRequest = new WorkOrderScheduleRequest();
            scheduleRequest.setAssignedTechnicianId(assignment.technicianId());
            scheduleRequest.setAssignedTeamId(assignment.teamId());
            scheduleRequest.setPlannedStartDate(window.start().toLocalDate());
            scheduleRequest.setPlannedStartTime(window.start().toLocalTime());
            scheduleRequest.setPlannedEndDate(window.end().toLocalDate());
            scheduleRequest.setPlannedEndTime(window.end().toLocalTime());
            scheduleRequest.setPlanner(SYSTEM_ACTOR);
            scheduleRequest.setPreCheckNotes("Auto-routed via voice AI intake");
            finalWorkOrder = workOrderService.scheduleWorkOrder(approvedWo.getId(), scheduleRequest);
        } else {
            escalationRequired = true;
            escalationReason = "No technician matched skill/location/availability; manual assignment required";
        }

        String assignedTechnicianName = finalWorkOrder.getAssignedTechnicianName();
        LocalDateTime expectedResponseAt = finalWorkOrder.getPlannedStartDateTime();
        String confirmationMessage = buildCustomerConfirmationMessage(
                approvedRequest.getRequestId(),
                finalWorkOrder.getWorkOrderId(),
                assignedTechnicianName,
                expectedResponseAt
        );

        VoiceIntakeOutcome outcome = finalWorkOrder.getStatus() == WorkOrderStatus.SCHEDULED
                ? VoiceIntakeOutcome.SERVICE_REQUEST_CREATED_AND_ROUTED
                : VoiceIntakeOutcome.SERVICE_REQUEST_CREATED;

        return ProcessingResult.builder()
                .outcome(outcome)
                .serviceRequest(approvedRequest)
                .workOrder(finalWorkOrder)
                .escalationRequired(escalationRequired)
                .escalationReason(escalationReason)
                .assignedTechnicianName(assignedTechnicianName)
                .expectedResponseAt(expectedResponseAt)
                .customerConfirmationMessage(confirmationMessage)
                .build();
    }

    private void validateServiceRequestPayload(StructuredInput structured) {
        if (isBlank(structured.requesterName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "requesterName is required for new service requests");
        }
        if (isBlank(structured.assetName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "assetName is required for new service requests");
        }
        if (isBlank(structured.issueDescription())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "issueDescription is required for new service requests");
        }
        if (isBlank(structured.location())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "location is required for new service requests");
        }
        if (structured.priority() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "urgency is required for new service requests");
        }
        if (!ALLOWED_URGENCIES.contains(structured.priority())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid urgency");
        }
        if (isBlank(structured.requesterPhoneNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "requesterPhoneNumber is required for new service requests");
        }
        int phoneLength = structured.requesterPhoneNumber().trim().length();
        if (phoneLength < 8 || phoneLength > 15) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid phone number");
        }
    }

    private ServiceRequestCreateDto buildServiceRequestCreateDto(StructuredInput structured) {
        ServiceRequestCreateDto dto = new ServiceRequestCreateDto();
        dto.setRequesterName(structured.requesterName());
        dto.setRequesterPhoneNumber(structured.requesterPhoneNumber());
        dto.setRequesterContact(structured.requesterContact());
        dto.setDepartment(structured.department());
        dto.setAssetId(structured.asset() != null ? structured.asset().getId() : null);
        dto.setLocation(trim(structured.location()));
        dto.setPriority(structured.priority());
        dto.setMaintenanceType(structured.maintenanceType());
        dto.setShortTitle(buildShortTitle(structured.shortTitle(), structured.assetName(), structured.issueDescription()));
        dto.setProblemDescription(buildProblemDescription(structured));
        dto.setPreferredTechnicianId(structured.preferredTechnicianId());
        dto.setPreferredTeamId(structured.preferredTeamId());
        dto.setSafetyRisk(structured.safetyRisk());

        if (structured.preferredStart() != null) {
            dto.setPreferredStartDate(structured.preferredStart().toLocalDate());
            dto.setPreferredStartTime(structured.preferredStart().toLocalTime());
        }
        if (structured.preferredEnd() != null) {
            dto.setPreferredEndDate(structured.preferredEnd().toLocalDate());
            dto.setPreferredEndTime(structured.preferredEnd().toLocalTime());
        }
        return dto;
    }

    private AssignmentDecision chooseAssignment(StructuredInput structured, Long companyId) {
        if (structured.preferredTeamId() != null) {
            ScheduleWindow window = resolveBestWindow(null, structured.preferredTeamId(), structured);
            return AssignmentDecision.builder()
                    .teamId(structured.preferredTeamId())
                    .window(window)
                    .build();
        }

        if (structured.preferredTechnicianId() != null) {
            ScheduleWindow window = resolveBestWindow(structured.preferredTechnicianId(), null, structured);
            String name = technicianRepository.findByIdAndIsDeletedFalseAndCompanyId(structured.preferredTechnicianId(), companyId)
                    .map(Technician::getFullName)
                    .orElse(null);
            return AssignmentDecision.builder()
                    .technicianId(structured.preferredTechnicianId())
                    .technicianName(name)
                    .window(window)
                    .build();
        }

        List<Technician> candidates = technicianRepository
                .findByIsDeletedFalseAndStatusAndCompanyId(TechnicianStatus.ACTIVE, companyId);
        if (candidates.isEmpty()) {
            return null;
        }

        List<CandidateScore> scored = candidates.stream()
                .map(tech -> scoreCandidate(tech, structured, companyId))
                .sorted(Comparator
                        .comparingInt(CandidateScore::skillScore).reversed()
                        .thenComparingInt(CandidateScore::locationScore).reversed()
                        .thenComparingLong(CandidateScore::activeBookings)
                        .thenComparingLong(CandidateScore::technicianId))
                .toList();

        for (CandidateScore candidate : scored) {
            ScheduleWindow window = resolveBestWindow(candidate.technicianId(), null, structured);
            if (window != null) {
                return AssignmentDecision.builder()
                        .technicianId(candidate.technicianId())
                        .technicianName(candidate.technicianName())
                        .window(window)
                        .build();
            }
        }

        CandidateScore fallback = scored.get(0);
        return AssignmentDecision.builder()
                .technicianId(fallback.technicianId())
                .technicianName(fallback.technicianName())
                .window(fallbackScheduleWindow(structured))
                .build();
    }

    private CandidateScore scoreCandidate(Technician technician, StructuredInput structured, Long companyId) {
        String skills = safeLower(technician.getSkills());
        String location = safeLower(technician.getAddress());

        int skillScore = 0;
        for (String tag : structured.skillTags()) {
            if (skills.contains(tag)) {
                skillScore++;
            }
        }
        if (skillScore == 0 && !isBlank(structured.issueDescription()) && skills.contains(safeLower(structured.issueDescription()))) {
            skillScore = 1;
        }

        int locationScore = 0;
        if (!isBlank(structured.location()) && location.contains(safeLower(structured.location()))) {
            locationScore = 1;
        }

        long activeBookings = workOrderRepository.countActiveBookingsForTechnician(
                technician.getId(),
                companyId,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(7),
                BOOKING_CONFLICT_STATUSES
        );

        return new CandidateScore(
                technician.getId(),
                technician.getFullName(),
                skillScore,
                locationScore,
                activeBookings
        );
    }

    private ScheduleWindow resolveBestWindow(Long technicianId, Long teamId, StructuredInput structured) {
        ScheduleWindow preferred = resolvePreferredWindow(structured);
        if (preferred != null && technicianId != null && isTechnicianAvailable(technicianId, preferred)) {
            return preferred;
        }
        if (preferred != null && teamId != null) {
            return preferred;
        }

        WorkOrderAvailabilityRequest availabilityRequest = new WorkOrderAvailabilityRequest();
        LocalDate startDate = preferred != null
                ? preferred.start().toLocalDate()
                : LocalDate.now();
        availabilityRequest.setStartDate(startDate);
        availabilityRequest.setEndDate(startDate.plusDays(7));
        availabilityRequest.setDaysRequired(1);
        availabilityRequest.setHoursRequired(structured.expectedDurationHours());
        availabilityRequest.setTechnicianId(technicianId);
        availabilityRequest.setTeamId(teamId);

        List<AvailabilitySlotResponse> slots = workOrderService.getAvailabilityTimeSlots(availabilityRequest);
        if (!slots.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            AvailabilitySlotResponse firstFuture = slots.stream()
                    .filter(slot -> slot.getStart() != null && (slot.getStart().isAfter(now) || slot.getStart().isEqual(now)))
                    .findFirst()
                    .orElse(null);
            if (firstFuture != null) {
                return new ScheduleWindow(firstFuture.getStart(), firstFuture.getEnd());
            }
        }

        return preferred;
    }

    private boolean isTechnicianAvailable(Long technicianId, ScheduleWindow preferred) {
        long conflicts = workOrderRepository.countActiveBookingsForTechnician(
                technicianId,
                CompanyContextHolder.getCompanyId().orElse(null),
                preferred.start(),
                preferred.end(),
                BOOKING_CONFLICT_STATUSES
        );
        return conflicts == 0L;
    }

    private ScheduleWindow resolvePreferredWindow(StructuredInput structured) {
        if (structured.preferredStart() == null || structured.preferredEnd() == null) {
            return null;
        }
        if (structured.preferredEnd().isBefore(structured.preferredStart())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "preferredEndDateTime must be after preferredStartDateTime");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = structured.preferredStart().isBefore(now) ? roundUpToNextHour(now) : structured.preferredStart();
        LocalDateTime end = start.plusHours(structured.expectedDurationHours());
        if (structured.preferredEnd().isAfter(start)) {
            end = structured.preferredEnd();
        }
        return new ScheduleWindow(start, end);
    }

    private ScheduleWindow fallbackScheduleWindow(StructuredInput structured) {
        LocalDateTime start = roundUpToNextHour(LocalDateTime.now().plusMinutes(30));
        LocalDateTime end = start.plusHours(structured.expectedDurationHours());
        return new ScheduleWindow(start, end);
    }

    private ServiceMaintenance findServiceRequestForInquiry(StructuredInput structured, Long companyId) {
        Long id = tryParseLong(structured.inquiryServiceRequestId());
        if (id != null) {
            return serviceMaintenanceRepository.findById(id)
                    .filter(sr -> Objects.equals(sr.getCompanyId(), companyId))
                    .orElse(null);
        }
        if (!isBlank(structured.inquiryServiceRequestId())) {
            return serviceMaintenanceRepository
                    .findTopByRequestIdAndCompanyIdOrderByIdDesc(structured.inquiryServiceRequestId().trim(), companyId)
                    .orElse(null);
        }
        return null;
    }

    private WorkOrder findWorkOrderForInquiry(StructuredInput structured, Long companyId, ServiceMaintenance sr) {
        if (sr != null && sr.getId() != null) {
            WorkOrder linked = workOrderRepository.findByLinkedRequest_IdAndCompanyId(sr.getId(), companyId).orElse(null);
            if (linked != null) {
                return linked;
            }
        }

        Long id = tryParseLong(structured.inquiryWorkOrderId());
        if (id != null) {
            return workOrderRepository.findByIdAndDeletedFalseAndCompanyId(id, companyId).orElse(null);
        }
        if (isBlank(structured.inquiryWorkOrderId())) {
            return null;
        }

        String ref = structured.inquiryWorkOrderId().trim();
        WorkOrder byCode = workOrderRepository
                .findTopByWorkOrderIdAndDeletedFalseAndCompanyIdOrderByIdDesc(ref, companyId)
                .orElse(null);
        if (byCode != null) {
            return byCode;
        }
        return workOrderRepository
                .findTopByWoNumberAndDeletedFalseAndCompanyIdOrderByIdDesc(ref, companyId)
                .orElse(null);
    }

    private VoiceAiIntake persistIntake(Long companyId, StructuredInput structured, ProcessingResult result) {
        ServiceMaintenance serviceRequest = result.serviceRequest() != null
                ? serviceMaintenanceRepository.findById(result.serviceRequest().getId()).orElse(null)
                : null;
        WorkOrder workOrder = result.workOrder() != null
                ? workOrderRepository.findById(result.workOrder().getId()).orElse(null)
                : null;

        VoiceAiIntake intake = VoiceAiIntake.builder()
                .companyId(companyId)
                .externalCallId(trim(structured.externalCallId()))
                .intent(structured.intent())
                .outcome(result.outcome())
                .requesterName(trim(structured.requesterName()))
                .requesterPhoneNumber(trim(structured.requesterPhoneNumber()))
                .requesterContact(trim(structured.requesterContact()))
                .department(trim(structured.department()))
                .issueDescription(trimToMax(structured.issueDescription(), 2000))
                .shortTitle(trimToMax(structured.shortTitle(), 255))
                .assetDbId(structured.asset() != null ? structured.asset().getId() : structured.assetDbId())
                .assetExternalId(trim(structured.assetId()))
                .assetName(trim(structured.assetName()))
                .location(trim(structured.location()))
                .requestPriority(structured.priority())
                .maintenanceType(structured.maintenanceType())
                .transcript(buildTranscript(structured.transcript(), structured.conversation()))
                .structuredSummary(buildStructuredSummary(structured))
                .skillTags(String.join(",", structured.skillTags()))
                .requiresEscalation(result.escalationRequired())
                .escalationReason(trim(result.escalationReason()))
                .assignedTechnicianId(result.workOrder() != null ? result.workOrder().getAssignedTechnicianId() : null)
                .assignedTechnicianName(trim(result.assignedTechnicianName()))
                .expectedResponseAt(result.expectedResponseAt())
                .customerConfirmationMessage(trimToMax(result.customerConfirmationMessage(), 1000))
                .statusSnapshot(buildStatusSnapshot(result))
                .serviceRequest(serviceRequest)
                .workOrder(workOrder)
                .build();
        return voiceAiIntakeRepository.save(intake);
    }

    private StructuredInput structureInput(VoiceIntakeRequest request, Long companyId) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request payload is required");
        }
        VoiceCallIntent intent = inferIntent(request);
        RequestPriority priority = resolvePriority(request, intent);
        MaintenanceType maintenanceType = resolveMaintenanceType(request, intent);
        int durationHours = resolveExpectedDurationHours(request.getExpectedDurationHours());
        LocalDateTime preferredStart = request.getPreferredStartDateTime();
        LocalDateTime preferredEnd = request.getPreferredEndDateTime();
        String transcript = trim(request.getTranscript());
        String conversation = serializeConversation(request.getConversation());

        Asset asset = resolveAsset(companyId, request.getAssetDbId(), request.getAssetId());

        List<String> tags = sanitizeSkillTags(request.getSkillTags());

        return new StructuredInput(
                trim(request.getExternalCallId()),
                intent,
                trim(request.getRequesterName()),
                trim(request.getRequesterPhoneNumber()),
                trim(request.getRequesterContact()),
                trim(request.getDepartment()),
                trim(request.getIssueDescription()),
                trim(request.getShortTitle()),
                transcript,
                conversation,
                request.getAssetDbId(),
                trim(request.getAssetId()),
                trim(request.getAssetName()),
                trim(request.getLocation()),
                priority,
                maintenanceType,
                request.getSafetyRisk(),
                preferredStart,
                preferredEnd,
                durationHours,
                request.getPreferredTechnicianId(),
                request.getPreferredTeamId(),
                tags,
                trim(request.getInquiryServiceRequestId()),
                trim(request.getInquiryWorkOrderId()),
                Boolean.TRUE.equals(request.getUnclearIntent()),
                asset
        );
    }

    private VoiceCallIntent inferIntent(VoiceIntakeRequest request) {
        return VoiceCallIntent.NEW_SERVICE_REQUEST;
    }

    private RequestPriority resolvePriority(VoiceIntakeRequest request, VoiceCallIntent intent) {
        if (request.getUrgency() != null) {
            return request.getUrgency();
        }
        return null;
    }

    private MaintenanceType resolveMaintenanceType(VoiceIntakeRequest request, VoiceCallIntent intent) {
        if (request.getMaintenanceType() != null) {
            return request.getMaintenanceType();
        }
        if (intent == VoiceCallIntent.EMERGENCY) {
            return MaintenanceType.EMERGENCY;
        }
        return MaintenanceType.CORRECTIVE;
    }

    private int resolveExpectedDurationHours(Integer requested) {
        if (requested == null || requested < 1) {
            return DEFAULT_EXPECTED_DURATION_HOURS;
        }
        return Math.min(requested, 24);
    }

    private Asset resolveAsset(Long companyId, Long assetDbId, String assetId) {
        if (assetDbId != null) {
            Asset byDbId = assetRepository.findByIdAndCompanyId(assetDbId, companyId).orElse(null);
            if (byDbId != null) {
                return byDbId;
            }
        }
        if (!isBlank(assetId)) {
            return assetRepository.findFirstByAssetIdAndCompanyId(assetId.trim(), companyId).orElse(null);
        }
        return null;
    }

    private String buildProblemDescription(StructuredInput structured) {
        StringBuilder sb = new StringBuilder();
        sb.append(structured.issueDescription());

        if (!isBlank(structured.transcript())) {
            sb.append("\n\nVoice Transcript: ");
            sb.append(structured.transcript());
        }
        if (!isBlank(structured.conversation())) {
            sb.append("\n\nVoice Conversation: ");
            sb.append(structured.conversation());
        }

        sb.append("\n\nVoice Metadata: intent=")
                .append(structured.intent())
                .append(", urgency=")
                .append(structured.priority())
                .append(", assetName=")
                .append(isBlank(structured.assetName()) ? "N/A" : structured.assetName())
                .append(", location=")
                .append(isBlank(structured.location()) ? "N/A" : structured.location());

        if (!structured.skillTags().isEmpty()) {
            sb.append(", skillTags=").append(String.join("|", structured.skillTags()));
        }

        return trimToMax(sb.toString(), MAX_SR_DESCRIPTION);
    }

    private String buildShortTitle(String title, String assetName, String issueDescription) {
        if (!isBlank(title)) {
            return trimToMax(title.trim(), 255);
        }
        if (isBlank(issueDescription)) {
            return isBlank(assetName) ? "Voice Service Request" : trimToMax(assetName.trim(), 255);
        }
        String issue = issueDescription.trim();
        String prefix = isBlank(assetName) ? null : assetName.trim();
        String core = issue.length() > 80 ? issue.substring(0, 80) : issue;
        String composed = prefix == null ? core : prefix + " - " + core;
        return trimToMax(composed, 255);
    }

    private String buildCustomerConfirmationMessage(String serviceRequestId,
                                                    String workOrderId,
                                                    String technicianName,
                                                    LocalDateTime expectedResponseAt) {
        StringBuilder message = new StringBuilder();
        message.append("Request ").append(serviceRequestId != null ? serviceRequestId : "created");
        if (!isBlank(workOrderId)) {
            message.append(" linked to work order ").append(workOrderId);
        }
        if (!isBlank(technicianName)) {
            message.append(". Assigned technician: ").append(technicianName);
        } else {
            message.append(". Technician assignment is in progress");
        }
        if (expectedResponseAt != null) {
            message.append(". Expected response: ").append(expectedResponseAt);
        }
        return message.toString();
    }

    private String buildStatusInquiryMessage(VoiceInquiryStatusResponse inquiry) {
        if (inquiry == null) {
            return "No status found";
        }
        StringBuilder sb = new StringBuilder("Status update:");
        if (!isBlank(inquiry.getServiceRequestId())) {
            sb.append(" SR ").append(inquiry.getServiceRequestId())
                    .append(" is ").append(inquiry.getServiceRequestStatus());
        }
        if (!isBlank(inquiry.getWorkOrderId())) {
            sb.append(". WO ").append(inquiry.getWorkOrderId())
                    .append(" is ").append(inquiry.getWorkOrderStatus());
        }
        if (!isBlank(inquiry.getAssignedTechnician())) {
            sb.append(". Assigned technician: ").append(inquiry.getAssignedTechnician());
        }
        if (inquiry.getExpectedResponseAt() != null) {
            sb.append(". Expected response: ").append(inquiry.getExpectedResponseAt());
        }
        return sb.toString();
    }

    private String buildStructuredSummary(StructuredInput structured) {
        return "intent=" + structured.intent()
                + "; requester=" + safe(structured.requesterName())
                + "; priority=" + structured.priority()
                + "; maintenanceType=" + structured.maintenanceType()
                + "; assetDbId=" + (structured.asset() != null ? structured.asset().getId() : structured.assetDbId())
                + "; location=" + safe(structured.location())
                + "; conversationAttached=" + !isBlank(structured.conversation());
    }

    private String buildStatusSnapshot(ProcessingResult result) {
        StringBuilder sb = new StringBuilder();
        if (result.serviceRequest() != null) {
            sb.append("serviceRequestId=").append(result.serviceRequest().getRequestId())
                    .append(", serviceRequestStatus=").append(result.serviceRequest().getStatus());
        }
        if (result.workOrder() != null) {
            if (!sb.isEmpty()) {
                sb.append("; ");
            }
            sb.append("workOrderId=").append(result.workOrder().getWorkOrderId())
                    .append(", workOrderStatus=").append(result.workOrder().getStatus());
        }
        if (result.inquiryStatus() != null) {
            if (!sb.isEmpty()) {
                sb.append("; ");
            }
            sb.append("inquiryStatus=").append(result.inquiryStatus());
        }
        if (sb.isEmpty()) {
            sb.append("no-linked-records");
        }
        return sb.toString();
    }

    private List<String> sanitizeSkillTags(List<String> rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> cleaned = new LinkedHashSet<>();
        for (String tag : rawTags) {
            if (tag == null) {
                continue;
            }
            String normalized = tag.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                cleaned.add(normalized);
            }
        }
        return List.copyOf(cleaned);
    }

    private boolean containsEmergencyHint(String description) {
        if (isBlank(description)) {
            return false;
        }
        String normalized = description.toLowerCase(Locale.ROOT);
        return EMERGENCY_HINTS.stream().anyMatch(normalized::contains);
    }

    private Long requireCompanyId() {
        return CompanyContextHolder.getCompanyId()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyId query parameter is required"));
    }

    private Long tryParseLong(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDateTime roundUpToNextHour(LocalDateTime dt) {
        LocalDateTime candidate = dt.withSecond(0).withNano(0);
        if (candidate.getMinute() == 0) {
            return candidate;
        }
        return candidate.plusHours(1).withMinute(0);
    }

    private String safeLower(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String serializeConversation(Object conversation) {
        if (conversation == null) {
            return null;
        }
        if (conversation instanceof String text) {
            return trim(text);
        }
        try {
            return trim(objectMapper.writeValueAsString(conversation));
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conversation must be a valid JSON value");
        }
    }

    private String buildTranscript(String transcript, String conversation) {
        if (isBlank(transcript) && isBlank(conversation)) {
            return null;
        }
        if (isBlank(conversation)) {
            return transcript;
        }
        if (isBlank(transcript)) {
            return "Conversation: " + conversation;
        }
        return transcript + "\n\nConversation: " + conversation;
    }

    private String trimToMax(String value, int maxLength) {
        String cleaned = trim(value);
        if (cleaned == null) {
            return null;
        }
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        return cleaned.substring(0, maxLength);
    }

    private String safe(String value) {
        return value == null ? "N/A" : value;
    }

    private String buildExternalRef(StructuredInput structured) {
        if (!isBlank(structured.externalCallId())) {
            return structured.externalCallId();
        }
        return "VOICE-" + LocalDateTime.now().withNano(0);
    }

    private record StructuredInput(
            String externalCallId,
            VoiceCallIntent intent,
            String requesterName,
            String requesterPhoneNumber,
            String requesterContact,
            String department,
            String issueDescription,
            String shortTitle,
            String transcript,
            String conversation,
            Long assetDbId,
            String assetId,
            String assetName,
            String location,
            RequestPriority priority,
            MaintenanceType maintenanceType,
            Boolean safetyRisk,
            LocalDateTime preferredStart,
            LocalDateTime preferredEnd,
            int expectedDurationHours,
            Long preferredTechnicianId,
            Long preferredTeamId,
            List<String> skillTags,
            String inquiryServiceRequestId,
            String inquiryWorkOrderId,
            boolean unclearIntent,
            Asset asset
    ) {
    }

    private record ScheduleWindow(LocalDateTime start, LocalDateTime end) {
    }

    private record CandidateScore(
            Long technicianId,
            String technicianName,
            int skillScore,
            int locationScore,
            long activeBookings
    ) {
    }

    @lombok.Builder
    private record AssignmentDecision(
            Long technicianId,
            Long teamId,
            String technicianName,
            ScheduleWindow window
    ) {
        boolean hasAssignment() {
            return technicianId != null || teamId != null;
        }
    }

    @lombok.Builder
    private record ProcessingResult(
            VoiceIntakeOutcome outcome,
            ServiceRequestResponse serviceRequest,
            WorkOrderDetailsResponse workOrder,
            VoiceInquiryStatusResponse inquiryStatus,
            boolean escalationRequired,
            String escalationReason,
            String assignedTechnicianName,
            LocalDateTime expectedResponseAt,
            String customerConfirmationMessage
    ) {
    }
}
