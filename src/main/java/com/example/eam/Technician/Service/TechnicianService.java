package com.example.eam.Technician.Service;

import com.example.eam.Enum.TechnicianStatus;
import com.example.eam.Enum.TechnicianType;
import com.example.eam.Enum.TechnicianWorkStatus;
import com.example.eam.Enum.TechnicianCalendarStatus;
import com.example.eam.Enum.WorkOrderStatus;
import com.example.eam.Technician.Dto.*;
import com.example.eam.Technician.Entity.Technician;
import com.example.eam.Technician.Entity.TechnicianLeave;
import com.example.eam.Technician.Entity.TechnicianHoliday;
import com.example.eam.Technician.Dto.TechnicianTeamMembershipResponse;
import com.example.eam.Technician.Repository.TechnicianRepository;
import com.example.eam.Technician.Repository.TechnicianLeaveRepository;
import com.example.eam.Technician.Repository.TechnicianHolidayRepository;
import com.example.eam.TechnicianTeam.Entity.TechnicianTeamMember;
import com.example.eam.TechnicianTeam.Repository.TechnicianTeamMemberRepository;
import com.example.eam.WorkOrder.Repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class TechnicianService {

    private final TechnicianRepository technicianRepository;
    private final TechnicianTeamMemberRepository teamMemberRepository;
    private final TechnicianLeaveRepository technicianLeaveRepository;
    private final TechnicianHolidayRepository technicianHolidayRepository;
    private final WorkOrderRepository workOrderRepository;

    @Transactional
    public TechnicianDetailsResponse createTechnician(TechnicianCreateRequest request) {
        String email = safeTrim(request.getEmail());
        if (email != null && technicianRepository.existsByEmailIgnoreCaseAndIsDeletedFalse(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Technician with the same email already exists");
        }

        String badgeNumber = requireBadgeUnique(request.getBadgeNumber());
        String technicianId = determineTechnicianId(request.getTechnicianId());


        TechnicianStatus status = request.getStatus() != null ? request.getStatus() : TechnicianStatus.ACTIVE;
        String firstName = request.getFirstName().trim();
        String lastName = request.getLastName().trim();

        Technician technician = Technician.builder()
                .firstName(firstName)
                .lastName(lastName)
                .badgeNumber(badgeNumber)
                .technicianId(technicianId)
                .technicianType(request.getTechnicianType())
                .skills(request.getSkills())
                .phoneNumber(safeTrim(request.getPhoneNumber()))
                .email(email)
                .address(request.getAddress())
                .status(status)
                .hireDate(request.getHireDate())
                .workShift(safeTrim(request.getWorkShift()))
                .technicianPhotoUrl(safeTrim(request.getTechnicianPhotoUrl()))
                .certificateUrl(safeTrim(request.getCertificateUrl()))
                .certificateIssueDate(request.getCertificateIssueDate())
                .certificateExpiryDate(request.getCertificateExpiryDate())
                .terminationDate(resolveTerminationDate(request.getTechnicianType(), request.getTerminationDate()))
                .certifications(request.getCertifications())
                .notes(request.getNotes())
                .build();

        Technician saved = technicianRepository.save(technician);
        return toDetailsResponse(saved);
    }

    @Transactional(readOnly = true)
    public TechnicianDetailsResponse getTechnician(Long id) {
        Technician technician = getTechnicianOrThrow(id);
        return toDetailsResponse(technician);
    }

    @Transactional(readOnly = true)
    public TechnicianListResponse listTechnicians(Pageable pageable) {
        Page<Technician> page = technicianRepository.findByIsDeletedFalse(pageable);
        List<TechnicianDetailsResponse> rows = page.getContent().stream()
                .map(this::toDetailsResponse)
                .toList();

        return TechnicianListResponse.builder()
                .technicians(rows)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public List<DailyAvailabilityDto> getMonthlyAvailability(Long technicianId, Integer daysAhead) {
        Technician tech = getTechnicianOrThrow(technicianId);
        int days = (daysAhead == null || daysAhead < 1) ? 31 : daysAhead;

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(days); // exclusive

        // Seed calendar with base status (Sunday -> HOLIDAY, others -> AVAILABLE)
        Map<LocalDate, TechnicianCalendarStatus> calendar = new HashMap<>();
        for (LocalDate d = startDate; d.isBefore(endDate); d = d.plusDays(1)) {
            TechnicianCalendarStatus base = d.getDayOfWeek().getValue() == 7
                    ? TechnicianCalendarStatus.HOLIDAY
                    : TechnicianCalendarStatus.AVAILABLE;
            calendar.put(d, base);
        }

        // Mark working days from booked work orders (direct or via team)
        List<com.example.eam.WorkOrder.Entity.WorkOrder> bookings =
                workOrderRepository.findBookingsForTechnicianCalendar(
                        technicianId,
                        startDate.atStartOfDay(),
                        endDate.atStartOfDay(),
                        EnumSet.of(WorkOrderStatus.SCHEDULED, WorkOrderStatus.IN_PROGRESS)
                );

        for (var wo : bookings) {
            LocalDate workStart = wo.getPlannedStartDateTime().toLocalDate();
            LocalDate workEnd = wo.getPlannedEndDateTime().toLocalDate();
            LocalDate cursor = workStart;
            while (!cursor.isAfter(workEnd)) {
                if (!cursor.isBefore(startDate) && cursor.isBefore(endDate)) {
                    calendar.put(cursor, TechnicianCalendarStatus.WORKING);
                }
                cursor = cursor.plusDays(1);
            }
        }

        applyLeavesToCalendar(calendar, technicianId, startDate, endDate);
        applyHolidaysToCalendar(calendar, startDate, endDate);

        List<DailyAvailabilityDto> result = new ArrayList<>();
        for (LocalDate d = startDate; d.isBefore(endDate); d = d.plusDays(1)) {
            LocalDateTime dayStart = d.atTime(9, 0);
            LocalDateTime dayEnd = d.atTime(21, 0);

            List<TimeWindow> busyWindows = new ArrayList<>();
            for (var wo : bookings) {
                LocalDateTime woStart = wo.getPlannedStartDateTime();
                LocalDateTime woEnd = wo.getPlannedEndDateTime();
                if (woStart == null || woEnd == null) continue;

                if (woEnd.isAfter(dayStart) && woStart.isBefore(dayEnd)) {
                    LocalDateTime busyStart = woStart.isAfter(dayStart) ? woStart : dayStart;
                    LocalDateTime busyEnd = woEnd.isBefore(dayEnd) ? woEnd : dayEnd;
                    if (busyStart.isBefore(busyEnd)) {
                        busyWindows.add(new TimeWindow(busyStart, busyEnd));
                    }
                }
            }

            List<TimeWindow> mergedBusy = mergeIntervals(
                    busyWindows.stream()
                            .sorted((a, b) -> a.start().compareTo(b.start()))
                            .toList()
            );

            TechnicianCalendarStatus status = calendar.get(d);

            List<TimeWindow> freeWindows = switch (status) {
                case HOLIDAY, PTO -> List.of();
                default -> computeFreeWindows(dayStart, dayEnd, mergedBusy);
            };

            List<TimeWindowDto> busyDtos = switch (status) {
                case HOLIDAY, PTO -> List.of(TimeWindowDto.builder()
                        .start(dayStart.toLocalTime())
                        .end(dayEnd.toLocalTime())
                        .build());
                default -> mergedBusy.stream()
                        .map(w -> TimeWindowDto.builder()
                                .start(w.start().toLocalTime())
                                .end(w.end().toLocalTime())
                                .build())
                        .toList();
            };

            List<TimeWindowDto> freeDtos = freeWindows.stream()
                    .map(w -> TimeWindowDto.builder()
                            .start(w.start().toLocalTime())
                            .end(w.end().toLocalTime())
                            .build())
                    .toList();

            result.add(DailyAvailabilityDto.builder()
                    .date(d)
                    .status(status)
                    .busyWindows(busyDtos)
                    .freeWindows(freeDtos)
                    .build());
        }
        return result;
    }

    @Transactional
    public TechnicianLeaveResponse applyLeave(Long technicianId, TechnicianLeaveRequest request) {
        Technician technician = getTechnicianOrThrow(technicianId);

        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();
        if (start == null || end == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate and endDate are required");
        }
        if (end.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate cannot be before startDate");
        }

        boolean overlaps = technicianLeaveRepository
                .existsByTechnician_IdAndEndDateGreaterThanEqualAndStartDateLessThanEqual(technicianId, start, end);
        if (overlaps) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Leave dates overlap with an existing leave");
        }

        TechnicianLeave leave = TechnicianLeave.builder()
                .technician(technician)
                .startDate(start)
                .endDate(end)
                .reason(safeTrim(request.getReason()))
                .build();

        TechnicianLeave saved = technicianLeaveRepository.save(leave);
        return toLeaveResponse(saved);
    }

    @Transactional(readOnly = true)
    public TechnicianLeaveResponse getLeave(Long technicianId, Long leaveId) {
        TechnicianLeave leave = technicianLeaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave not found"));
        if (!leave.getTechnician().getId().equals(technicianId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave not found");
        }
        return toLeaveResponse(leave);
    }

    @Transactional(readOnly = true)
    public TechnicianLeaveListResponse listLeaves(Long technicianId, LocalDate startDate, LocalDate endDate) {
        Technician tech = getTechnicianOrThrow(technicianId);
        List<TechnicianLeave> leaves;
        if (startDate != null && endDate != null) {
            LocalDate endExclusive = endDate.plusDays(1);
            leaves = technicianLeaveRepository.findOverlapping(tech.getId(), startDate, endExclusive);
        } else {
            leaves = technicianLeaveRepository.findByTechnician_IdOrderByStartDateAsc(tech.getId());
        }
        List<TechnicianLeaveResponse> responses = leaves.stream()
                .map(this::toLeaveResponse)
                .toList();
        return TechnicianLeaveListResponse.builder()
                .leaves(responses)
                .build();
    }

    @Transactional(readOnly = true)
    public TechnicianLeaveListResponse listAllLeaves(LocalDate startDate, LocalDate endDate) {
        List<TechnicianLeave> leaves;
        if (startDate != null && endDate != null) {
            LocalDate endExclusive = endDate.plusDays(1);
            leaves = technicianLeaveRepository.findOverlappingAny(startDate, endExclusive);
        } else {
            leaves = technicianLeaveRepository.findAllByOrderByStartDateAsc();
        }
        List<TechnicianLeaveResponse> responses = leaves.stream()
                .map(this::toLeaveResponse)
                .toList();
        return TechnicianLeaveListResponse.builder()
                .leaves(responses)
                .build();
    }

    @Transactional
    public TechnicianLeaveResponse patchLeave(Long technicianId, Long leaveId, TechnicianLeavePatchRequest request) {
        TechnicianLeave leave = technicianLeaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave not found"));
        if (!leave.getTechnician().getId().equals(technicianId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave not found");
        }

        LocalDate newStart = request.getStartDate() != null ? request.getStartDate() : leave.getStartDate();
        LocalDate newEnd = request.getEndDate() != null ? request.getEndDate() : leave.getEndDate();
        if (newStart == null || newEnd == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate and endDate are required");
        }
        if (newEnd.isBefore(newStart)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate cannot be before startDate");
        }

        boolean overlaps = technicianLeaveRepository
                .existsByTechnician_IdAndEndDateGreaterThanEqualAndStartDateLessThanEqualAndIdNot(
                        technicianId, newStart, newEnd, leaveId);
        if (overlaps) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Leave dates overlap with an existing leave");
        }

        leave.setStartDate(newStart);
        leave.setEndDate(newEnd);
        if (request.getReason() != null) {
            leave.setReason(safeTrim(request.getReason()));
        }

        TechnicianLeave saved = technicianLeaveRepository.save(leave);
        return toLeaveResponse(saved);
    }

    @Transactional
    public void deleteLeave(Long technicianId, Long leaveId) {
        TechnicianLeave leave = technicianLeaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave not found"));
        if (!leave.getTechnician().getId().equals(technicianId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave not found");
        }
        technicianLeaveRepository.delete(leave);
    }

    @Transactional
    public TechnicianHolidayResponse addHoliday(TechnicianHolidayRequest request) {
        if (technicianHolidayRepository.existsByHolidayDate(request.getHolidayDate())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Holiday already exists for the given date");
        }

        TechnicianHoliday holiday = TechnicianHoliday.builder()
                .holidayName(request.getHolidayName().trim())
                .holidayType(request.getHolidayType())
                .holidayDate(request.getHolidayDate())
                .notes(safeTrim(request.getNotes()))
                .build();

        TechnicianHoliday saved = technicianHolidayRepository.save(holiday);
        return toHolidayResponse(saved);
    }

    @Transactional(readOnly = true)
    public TechnicianHolidayResponse getHoliday(Long id) {
        TechnicianHoliday holiday = technicianHolidayRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Holiday not found"));
        return toHolidayResponse(holiday);
    }

    @Transactional(readOnly = true)
    public TechnicianHolidayListResponse listHolidays(LocalDate startDate, LocalDate endDate) {
        List<TechnicianHoliday> rows;
        if (startDate != null && endDate != null) {
            LocalDate endExclusive = endDate.plusDays(1);
            rows = technicianHolidayRepository.findInRange(startDate, endExclusive);
        } else {
            rows = technicianHolidayRepository.findAllByOrderByHolidayDateAsc();
        }
        List<TechnicianHolidayResponse> responses = rows.stream()
                .map(this::toHolidayResponse)
                .toList();
        return TechnicianHolidayListResponse.builder()
                .holidays(responses)
                .build();
    }

    @Transactional
    public TechnicianHolidayResponse patchHoliday(Long id, TechnicianHolidayPatchRequest request) {
        TechnicianHoliday holiday = technicianHolidayRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Holiday not found"));

        if (request.getHolidayName() != null) {
            String name = request.getHolidayName().trim();
            if (name.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "holidayName cannot be blank");
            }
            holiday.setHolidayName(name);
        }

        if (request.getHolidayType() != null) {
            holiday.setHolidayType(request.getHolidayType());
        }

        if (request.getHolidayDate() != null) {
            LocalDate newDate = request.getHolidayDate();
            if (technicianHolidayRepository.existsByHolidayDateAndIdNot(newDate, id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Holiday already exists for the given date");
            }
            holiday.setHolidayDate(newDate);
        }

        if (request.getNotes() != null) {
            holiday.setNotes(safeTrim(request.getNotes()));
        }

        TechnicianHoliday saved = technicianHolidayRepository.save(holiday);
        return toHolidayResponse(saved);
    }

    @Transactional
    public void deleteHoliday(Long id) {
        TechnicianHoliday holiday = technicianHolidayRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Holiday not found"));
        technicianHolidayRepository.delete(holiday);
    }

    @Transactional
    public TechnicianDetailsResponse patchTechnician(Long id, TechnicianPatchRequest request) {
        Technician technician = getTechnicianOrThrow(id);

        if (request.getFirstName() != null) {
            String firstName = request.getFirstName().trim();
            if (firstName.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "First name cannot be blank");
            }
            technician.setFirstName(firstName);
        }

        if (request.getLastName() != null) {
            String lastName = request.getLastName().trim();
            if (lastName.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Last name cannot be blank");
            }
            technician.setLastName(lastName);
        }
        if (request.getTechnicianType() != null) technician.setTechnicianType(request.getTechnicianType());
        if (request.getSkills() != null) technician.setSkills(request.getSkills());
        if (request.getPhoneNumber() != null) technician.setPhoneNumber(safeTrim(request.getPhoneNumber()));

        if (request.getBadgeNumber() != null) {
            String badge = requireBadgeUnique(request.getBadgeNumber(), technician.getId());
            technician.setBadgeNumber(badge);
        }

        if (request.getTechnicianId() != null) {
            String techId = request.getTechnicianId().trim();
            if (techId.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "technicianId cannot be blank");
            }
            ensureTechnicianIdUnique(techId, technician.getId());
            technician.setTechnicianId(techId);
        }

        if (request.getEmail() != null) {
            String email = safeTrim(request.getEmail());
            if (email != null && !email.equalsIgnoreCase(safeTrim(technician.getEmail()))
                    && technicianRepository.existsByEmailIgnoreCaseAndIsDeletedFalse(email)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Technician with the same email already exists");
            }
            technician.setEmail(email);
        }

        if (request.getAddress() != null) technician.setAddress(request.getAddress());
        if (request.getStatus() != null) technician.setStatus(request.getStatus());
        if (request.getHireDate() != null) technician.setHireDate(request.getHireDate());
        if (request.getWorkShift() != null) technician.setWorkShift(safeTrim(request.getWorkShift()));
        if (request.getTechnicianPhotoUrl() != null) technician.setTechnicianPhotoUrl(safeTrim(request.getTechnicianPhotoUrl()));
        if (request.getCertificateUrl() != null) technician.setCertificateUrl(safeTrim(request.getCertificateUrl()));
        if (request.getCertificateIssueDate() != null) technician.setCertificateIssueDate(request.getCertificateIssueDate());
        if (request.getCertificateExpiryDate() != null) technician.setCertificateExpiryDate(request.getCertificateExpiryDate());
        if (request.getTerminationDate() != null || request.getTechnicianType() != null) {
            TechnicianType type = request.getTechnicianType() != null ? request.getTechnicianType() : technician.getTechnicianType();
            technician.setTerminationDate(resolveTerminationDate(type, request.getTerminationDate() != null ? request.getTerminationDate() : technician.getTerminationDate()));
        }
        if (request.getCertifications() != null) technician.setCertifications(request.getCertifications());
        if (request.getNotes() != null) technician.setNotes(request.getNotes());

        Technician saved = technicianRepository.save(technician);
        return toDetailsResponse(saved);
    }

    @Transactional
    public void deleteTechnician(Long id) {
        Technician technician = getTechnicianOrThrow(id);
        LocalDateTime now = LocalDateTime.now();
        long activeOrFutureBookings = workOrderRepository.countActiveBookingsForTechnician(
                id,
                now,
                now.plusYears(50), // generous window for "future"
                EnumSet.of(WorkOrderStatus.SCHEDULED, WorkOrderStatus.IN_PROGRESS)
        );
        if (activeOrFutureBookings > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot delete technician while assigned to active work orders or in future work orders"
            );
        }
        // Remove team memberships to avoid dangling references to soft-deleted technicians
        teamMemberRepository.deleteByTechnician_Id(id);
        technician.setDeleted(true);
        technicianRepository.save(technician);
    }

    private Technician getTechnicianOrThrow(Long id) {
        return technicianRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Technician not found"));
    }

    private TechnicianDetailsResponse toDetailsResponse(Technician technician) {
        List<TechnicianTeamMembershipResponse> teamMemberships = buildTeamMemberships(technician);
        TechnicianWorkStatus workStatusToday = computeWorkStatusToday(technician.getId());

        return TechnicianDetailsResponse.builder()
                .id(technician.getId())
                .firstName(technician.getFirstName())
                .lastName(technician.getLastName())
                .fullName(technician.getFullName())
                .badgeNumber(technician.getBadgeNumber())
                .technicianId(technician.getTechnicianId())
                .technicianType(technician.getTechnicianType())
                .skills(technician.getSkills())
                .phoneNumber(technician.getPhoneNumber())
                .email(technician.getEmail())
                .address(technician.getAddress())
                .status(technician.getStatus())
                .workStatus(workStatusToday)
                .hireDate(technician.getHireDate())
                .workShift(technician.getWorkShift())
                .technicianPhotoUrl(technician.getTechnicianPhotoUrl())
                .certificateUrl(technician.getCertificateUrl())
                .certificateIssueDate(technician.getCertificateIssueDate())
                .certificateExpiryDate(technician.getCertificateExpiryDate())
                .terminationDate(technician.getTerminationDate())
                .certifications(technician.getCertifications())
                .notes(technician.getNotes())
                .teamLeader(teamMemberships.stream().anyMatch(TechnicianTeamMembershipResponse::isTeamLeader))
                .teamMemberships(teamMemberships)
                .build();
    }

    private TechnicianLeaveResponse toLeaveResponse(TechnicianLeave leave) {
        Technician tech = leave.getTechnician();
        String fullName = tech.getFullName();
        if (fullName == null || fullName.isBlank()) {
            String first = tech.getFirstName() != null ? tech.getFirstName() : "";
            String last = tech.getLastName() != null ? tech.getLastName() : "";
            fullName = (first + " " + last).trim();
        }

        return TechnicianLeaveResponse.builder()
                .id(leave.getId())
                .technicianId(tech.getId())
                .technicianName(fullName == null || fullName.isBlank() ? null : fullName)
                .startDate(leave.getStartDate())
                .endDate(leave.getEndDate())
                .reason(leave.getReason())
                .createdAt(leave.getCreatedAt())
                .build();
    }

    private TechnicianHolidayResponse toHolidayResponse(TechnicianHoliday holiday) {
        return TechnicianHolidayResponse.builder()
                .id(holiday.getId())
                .holidayName(holiday.getHolidayName())
                .holidayType(holiday.getHolidayType())
                .holidayDate(holiday.getHolidayDate())
                .notes(holiday.getNotes())
                .createdAt(holiday.getCreatedAt())
                .build();
    }

    private TechnicianWorkStatus computeWorkStatusToday(Long technicianId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        long activeBookings = workOrderRepository.countActiveBookingsForTechnician(
                technicianId,
                start,
                end,
                EnumSet.of(WorkOrderStatus.SCHEDULED, WorkOrderStatus.IN_PROGRESS)
        );

        return activeBookings > 0 ? TechnicianWorkStatus.WORKING : TechnicianWorkStatus.AVAILABLE;
    }

    private void applyLeavesToCalendar(Map<LocalDate, TechnicianCalendarStatus> calendar,
                                       Long technicianId,
                                       LocalDate rangeStart,
                                       LocalDate rangeEndExclusive) {
        List<TechnicianLeave> leaves = technicianLeaveRepository.findOverlapping(technicianId, rangeStart, rangeEndExclusive);
        for (TechnicianLeave leave : leaves) {
            LocalDate leaveStart = leave.getStartDate().isBefore(rangeStart) ? rangeStart : leave.getStartDate();
            LocalDate leaveEnd = leave.getEndDate().isBefore(rangeEndExclusive.minusDays(1))
                    ? leave.getEndDate()
                    : rangeEndExclusive.minusDays(1);

            for (LocalDate d = leaveStart; !d.isAfter(leaveEnd); d = d.plusDays(1)) {
                calendar.put(d, TechnicianCalendarStatus.PTO);
            }
        }
    }

    private void applyHolidaysToCalendar(Map<LocalDate, TechnicianCalendarStatus> calendar,
                                         LocalDate rangeStart,
                                         LocalDate rangeEndExclusive) {
        List<TechnicianHoliday> holidays = technicianHolidayRepository.findInRange(rangeStart, rangeEndExclusive);
        for (TechnicianHoliday holiday : holidays) {
            LocalDate date = holiday.getHolidayDate();
            calendar.put(date, TechnicianCalendarStatus.HOLIDAY);
        }
    }

    private record TimeWindow(LocalDateTime start, LocalDateTime end) { }

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

    private List<TechnicianTeamMembershipResponse> buildTeamMemberships(Technician technician) {
        List<TechnicianTeamMember> memberships = technician.getTeamMemberships();
        if (memberships == null || memberships.isEmpty()) {
            memberships = teamMemberRepository.findByTechnician_Id(technician.getId());
        }

        return memberships.stream()
                .map(membership -> TechnicianTeamMembershipResponse.builder()
                        .teamId(membership.getTeam().getId())
                        .teamName(membership.getTeam().getTeamName())
                        .teamLeader(membership.isTeamLeader())
                        .teamLeaderNames(resolveTeamLeaderNames(membership.getTeam().getId()))
                        .build())
                .toList();
    }

    private String safeTrim(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String requireBadgeUnique(String badgeNumber) {
        return requireBadgeUnique(badgeNumber, null);
    }

    private String requireBadgeUnique(String badgeNumber, Long currentId) {
        if (badgeNumber == null || badgeNumber.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "badgeNumber is required");
        }
        String trimmed = badgeNumber.trim();
        if (technicianRepository.existsByBadgeNumberIgnoreCaseAndIsDeletedFalse(trimmed)) {
            if (currentId == null || technicianRepository.findByIdAndIsDeletedFalse(currentId).stream()
                    .noneMatch(t -> trimmed.equalsIgnoreCase(t.getBadgeNumber()))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "badgeNumber already exists");
            }
        }
        return trimmed;
    }

    private String determineTechnicianId(String provided) {
        if (provided != null && !provided.trim().isEmpty()) {
            String trimmed = provided.trim();
            ensureTechnicianIdUnique(trimmed, null);
            return trimmed;
        }
        // simple auto id: TECH-YYYYMMDD-XXXX
        String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        for (int i = 0; i < 30; i++) {
            int rand = java.util.concurrent.ThreadLocalRandom.current().nextInt(0, 10000);
            String candidate = String.format("TECH-%s-%04d", date, rand);
            if (!technicianRepository.existsByTechnicianIdIgnoreCaseAndIsDeletedFalse(candidate)) {
                return candidate;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate technicianId");
    }

    private void ensureTechnicianIdUnique(String technicianId, Long currentId) {
        boolean exists = technicianRepository.existsByTechnicianIdIgnoreCaseAndIsDeletedFalse(technicianId);
        if (exists) {
            if (currentId == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "technicianId already exists");
            }
            technicianRepository.findByIdAndIsDeletedFalse(currentId).ifPresent(t -> {
                if (!technicianId.equalsIgnoreCase(t.getTechnicianId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "technicianId already exists");
                }
            });
        }
    }

    private LocalDate resolveTerminationDate(TechnicianType type, LocalDate terminationDate) {
        if (type == TechnicianType.CONTRACT) {
            return terminationDate;
        }
        if (terminationDate != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "terminationDate allowed only for CONTRACT technicians");
        }
        return null;
    }

    private List<String> resolveTeamLeaderNames(Long teamId) {
        return teamMemberRepository.findByTeam_Id(teamId).stream()
                .filter(TechnicianTeamMember::isTeamLeader)
                .map(tm -> {
                    var tech = tm.getTechnician();
                    if (tech.getFullName() != null && !tech.getFullName().isBlank()) return tech.getFullName();
                    String first = tech.getFirstName() != null ? tech.getFirstName() : "";
                    String last = tech.getLastName() != null ? tech.getLastName() : "";
                    return (first + " " + last).trim();
                })
                .filter(name -> name != null && !name.isBlank())
                .toList();
    }
}
