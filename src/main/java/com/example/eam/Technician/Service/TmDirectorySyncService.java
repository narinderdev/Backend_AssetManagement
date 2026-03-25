package com.example.eam.Technician.Service;

import com.example.eam.Enum.TechnicianStatus;
import com.example.eam.Enum.TechnicianTeamStatus;
import com.example.eam.Enum.TechnicianType;
import com.example.eam.Technician.Entity.Technician;
import com.example.eam.Technician.Repository.TechnicianRepository;
import com.example.eam.TechnicianTeam.Entity.TechnicianTeam;
import com.example.eam.TechnicianTeam.Entity.TechnicianTeamMember;
import com.example.eam.TechnicianTeam.Repository.TechnicianTeamMemberRepository;
import com.example.eam.TechnicianTeam.Repository.TechnicianTeamRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Periodically pulls technicians and teams from the TM database into the local EAM tables.
 * Writes only to the EAM DB; TM is treated as read-only.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(name = "tmJdbcTemplate")
public class TmDirectorySyncService {

    @Qualifier("tmJdbcTemplate")
    private final JdbcTemplate tmJdbcTemplate;
    private final TechnicianRepository technicianRepository;
    private final TechnicianTeamRepository technicianTeamRepository;
    private final TechnicianTeamMemberRepository technicianTeamMemberRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void syncAtStartup() {
        try {
            syncFromTm();
        } catch (Exception ex) {
            log.warn("TM directory sync at startup failed: {}", ex.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${tm.sync.interval-ms:300000}")
    public void scheduledSync() {
        try {
            syncFromTm();
        } catch (Exception ex) {
            log.warn("TM directory scheduled sync failed: {}", ex.getMessage());
        }
    }

    /**
     * Run a full sync. Safe to call on-demand (idempotent).
     */
    @Transactional
    public SyncReport syncFromTm() {
        List<TmUserRow> tmTechnicians = fetchTechnicians();
        List<TmTeamRow> tmTeams = fetchTeams();
        Map<Long, List<TmMemberRow>> membersByTeam = fetchMembersByTeam();

        Map<TmTechnicianKey, Technician> tmIdToTechnician = upsertTechnicians(tmTechnicians);
        upsertTeams(tmTeams, membersByTeam, tmIdToTechnician);

        SyncReport report = new SyncReport(tmIdToTechnician.size(), tmTeams.size());
        log.info("TM directory sync completed. technicians={}, teams={}", report.technicians(), report.teams());
        return report;
    }

    // ---------- Pull from TM ----------

    private List<TmUserRow> fetchTechnicians() {
        return tmJdbcTemplate.query("""
                SELECT id, company_id, first_name, last_name, email, status, is_deleted
                FROM technicians
                WHERE is_deleted = 0
                  AND company_id IS NOT NULL
                """, (rs, row) -> new TmUserRow(
                rs.getLong("id"),
                rs.getLong("company_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("email"),
                !"INACTIVE".equalsIgnoreCase(rs.getString("status")) && !rs.getBoolean("is_deleted")));
    }

    private List<TmTeamRow> fetchTeams() {
        return tmJdbcTemplate.query("""
                SELECT id, company_id, team_name, team_description, status
                FROM technician_teams
                WHERE company_id IS NOT NULL
                """, (rs, row) -> new TmTeamRow(
                rs.getLong("id"),
                rs.getLong("company_id"),
                rs.getString("team_name"),
                rs.getString("team_description"),
                rs.getString("status")));
    }

    private Map<Long, List<TmMemberRow>> fetchMembersByTeam() {
        return tmJdbcTemplate.query("""
                SELECT m.team_id, m.technician_id, m.team_leader, tt.company_id
                FROM technician_team_members m
                JOIN technician_teams tt ON tt.id = m.team_id
                JOIN technicians t ON t.id = m.technician_id
                WHERE tt.company_id IS NOT NULL
                  AND t.is_deleted = 0
                  AND t.company_id = tt.company_id
                """, (rs, row) -> new TmMemberRow(
                rs.getLong("team_id"),
                rs.getLong("company_id"),
                rs.getLong("technician_id"),
                rs.getBoolean("team_leader")))
                .stream()
                .collect(Collectors.groupingBy(TmMemberRow::teamId));
    }

    // ---------- Upsert into EAM ----------

    private Map<TmTechnicianKey, Technician> upsertTechnicians(List<TmUserRow> rows) {
        Map<TmTechnicianKey, Technician> result = new HashMap<>();
        for (TmUserRow row : rows) {
            String externalId = externalTechId(row.id());
            String email = row.email();

            Optional<Technician> existingById = technicianRepository
                    .findFirstByTechnicianIdIgnoreCaseAndCompanyId(externalId, row.companyId());
            Optional<Technician> existingByEmail = email == null
                    ? Optional.empty()
                    : technicianRepository.findFirstByEmailIgnoreCaseAndCompanyId(email, row.companyId());

            Technician tech = existingById
                    .or(() -> existingByEmail)
                    .or(() -> findLegacyTechnician(row.companyId(), externalId, email))
                    .orElseGet(Technician::new);

            tech.setCompanyId(row.companyId());
            tech.setTechnicianId(externalId);
            if (tech.getBadgeNumber() == null || tech.getBadgeNumber().isBlank()) {
                tech.setBadgeNumber("TM-BADGE-" + row.id());
            }
            tech.setFirstName(defaulted(row.firstName(), "Technician"));
            tech.setLastName(defaulted(row.lastName(), String.valueOf(row.id())));
            tech.setEmail(email);
            tech.setTechnicianType(TechnicianType.FULL_TIME);
            tech.setStatus(row.active() ? TechnicianStatus.ACTIVE : TechnicianStatus.INACTIVE);
            tech.setDeleted(!row.active());

            Technician saved = technicianRepository.save(tech);
            result.put(new TmTechnicianKey(row.companyId(), row.id()), saved);
        }
        return result;
    }

    private void upsertTeams(List<TmTeamRow> teams,
                             Map<Long, List<TmMemberRow>> members,
                             Map<TmTechnicianKey, Technician> tmIdToTech) {

        Map<TmTeamKey, TechnicianTeam> existingByName = technicianTeamRepository.findAll().stream()
                .filter(t -> t.getCompanyId() != null && t.getTeamName() != null && !t.getTeamName().isBlank())
                .collect(Collectors.toMap(
                        t -> new TmTeamKey(t.getCompanyId(), t.getTeamName().toLowerCase(Locale.ROOT)),
                        Function.identity(), (a, b) -> a));

        for (TmTeamRow row : teams) {
            if (row.name() == null || row.name().isBlank()) {
                continue;
            }
            TmTeamKey key = new TmTeamKey(row.companyId(), row.name().toLowerCase(Locale.ROOT));
            TechnicianTeam team = existingByName.getOrDefault(key, new TechnicianTeam());
            team.setCompanyId(row.companyId());
            team.setTeamName(row.name());
            team.setTeamDescription(row.description());
            team.setStatus(parseStatus(row.status()));

            TechnicianTeam saved = technicianTeamRepository.save(team);
            existingByName.put(key, saved);
            syncMembers(saved, members.getOrDefault(row.id(), List.of()), tmIdToTech);
        }
    }

    private void syncMembers(TechnicianTeam team,
                             List<TmMemberRow> desired,
                             Map<TmTechnicianKey, Technician> tmIdToTech) {

        Map<Long, TechnicianTeamMember> current = technicianTeamMemberRepository.findByTeam_Id(team.getId())
                .stream()
                .collect(Collectors.toMap(m -> m.getTechnician().getId(), Function.identity()));

        Set<Long> keep = new HashSet<>();
        for (TmMemberRow row : desired) {
            if (!Objects.equals(team.getCompanyId(), row.companyId())) {
                continue;
            }
            Technician tech = tmIdToTech.get(new TmTechnicianKey(row.companyId(), row.technicianId()));
            if (tech == null) continue; // skip unknown

            keep.add(tech.getId());
            TechnicianTeamMember member = current.getOrDefault(tech.getId(), new TechnicianTeamMember());
            member.setTeam(team);
            member.setTechnician(tech);
            member.setTeamLeader(row.teamLeader());
            technicianTeamMemberRepository.save(member);
        }

        for (TechnicianTeamMember member : current.values()) {
            if (!keep.contains(member.getTechnician().getId())) {
                technicianTeamMemberRepository.delete(member);
            }
        }
    }

    // ---------- Helpers ----------

    private TechnicianTeamStatus parseStatus(String value) {
        if (value == null) return TechnicianTeamStatus.ACTIVE;
        try {
            return TechnicianTeamStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return TechnicianTeamStatus.ACTIVE;
        }
    }

    private String externalTechId(Long tmId) {
        return "TM-" + tmId;
    }

    private Optional<Technician> findLegacyTechnician(Long companyId, String externalId, String email) {
        Optional<Technician> byId = technicianRepository.findFirstByTechnicianIdIgnoreCase(externalId)
                .filter(technician -> technician.getCompanyId() == null || Objects.equals(technician.getCompanyId(), companyId));
        if (byId.isPresent()) {
            return byId;
        }
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return technicianRepository.findFirstByEmailIgnoreCase(email)
                .filter(technician -> technician.getCompanyId() == null || Objects.equals(technician.getCompanyId(), companyId));
    }

    private String defaulted(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }

    // ---------- DTOs ----------
    public record SyncReport(int technicians, int teams) {}
    private record TmTechnicianKey(Long companyId, Long technicianId) {}
    private record TmTeamKey(Long companyId, String teamName) {}
    private record TmUserRow(Long id, Long companyId, String firstName, String lastName, String email, boolean active) {}
    private record TmTeamRow(Long id, Long companyId, String name, String description, String status) {}
    private record TmMemberRow(Long teamId, Long companyId, Long technicianId, boolean teamLeader) {}
}
