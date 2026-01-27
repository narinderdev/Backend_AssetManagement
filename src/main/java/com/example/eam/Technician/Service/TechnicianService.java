package com.example.eam.Technician.Service;

import com.example.eam.Enum.TechnicianStatus;
import com.example.eam.Enum.TechnicianType;
import com.example.eam.Technician.Dto.*;
import com.example.eam.Technician.Entity.Technician;
import com.example.eam.Technician.Dto.TechnicianTeamMembershipResponse;
import com.example.eam.Technician.Repository.TechnicianRepository;
import com.example.eam.TechnicianTeam.Entity.TechnicianTeamMember;
import com.example.eam.TechnicianTeam.Repository.TechnicianTeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TechnicianService {

    private final TechnicianRepository technicianRepository;
    private final TechnicianTeamMemberRepository teamMemberRepository;

    @Transactional
    public TechnicianDetailsResponse createTechnician(TechnicianCreateRequest request) {
        String email = safeTrim(request.getEmail());
        if (email != null && technicianRepository.existsByEmailIgnoreCase(email)) {
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
        Page<Technician> page = technicianRepository.findAll(pageable);
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
                    && technicianRepository.existsByEmailIgnoreCase(email)) {
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
        technicianRepository.delete(technician);
    }

    private Technician getTechnicianOrThrow(Long id) {
        return technicianRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Technician not found"));
    }

    private TechnicianDetailsResponse toDetailsResponse(Technician technician) {
        List<TechnicianTeamMembershipResponse> teamMemberships = buildTeamMemberships(technician);

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
        if (technicianRepository.existsByBadgeNumberIgnoreCase(trimmed)) {
            if (currentId == null || technicianRepository.findById(currentId).stream()
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
            if (!technicianRepository.existsByTechnicianIdIgnoreCase(candidate)) {
                return candidate;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate technicianId");
    }

    private void ensureTechnicianIdUnique(String technicianId, Long currentId) {
        boolean exists = technicianRepository.existsByTechnicianIdIgnoreCase(technicianId);
        if (exists) {
            if (currentId == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "technicianId already exists");
            }
            technicianRepository.findById(currentId).ifPresent(t -> {
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
}
