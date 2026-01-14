package com.example.eam.Technician.Service;

import com.example.eam.Enum.TechnicianStatus;
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



        TechnicianStatus status = request.getStatus() != null ? request.getStatus() : TechnicianStatus.ACTIVE;
        String firstName = request.getFirstName().trim();
        String lastName = request.getLastName().trim();

        Technician technician = Technician.builder()
                .firstName(firstName)
                .lastName(lastName)
                .technicianType(request.getTechnicianType())
                .skills(request.getSkills())
                .phoneNumber(safeTrim(request.getPhoneNumber()))
                .email(email)
                .address(request.getAddress())
                .status(status)
                .hireDate(request.getHireDate())
                .workShift(safeTrim(request.getWorkShift()))
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
                .technicianType(technician.getTechnicianType())
                .skills(technician.getSkills())
                .phoneNumber(technician.getPhoneNumber())
                .email(technician.getEmail())
                .address(technician.getAddress())
                .status(technician.getStatus())
                .hireDate(technician.getHireDate())
                .workShift(technician.getWorkShift())
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

}
