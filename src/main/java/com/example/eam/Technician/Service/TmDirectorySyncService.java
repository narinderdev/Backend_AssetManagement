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

        Map<Long, Technician> tmIdToTechnician = upsertTechnicians(tmTechnicians);
        upsertTeams(tmTeams, membersByTeam, tmIdToTechnician);

        SyncReport report = new SyncReport(tmIdToTechnician.size(), tmTeams.size());
        log.info("TM directory sync completed. technicians={}, teams={}", report.technicians(), report.teams());
        return report;
    }

    // ---------- Pull from TM ----------

    private List<TmUserRow> fetchTechnicians() {
        return tmJdbcTemplate.query("""
                SELECT id, first_name, last_name, email, status, is_deleted
                FROM technicians
                WHERE is_deleted = 0
                """, (rs, row) -> new TmUserRow(
                rs.getLong("id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("email"),
                !"INACTIVE".equalsIgnoreCase(rs.getString("status")) && !rs.getBoolean("is_deleted")));
    }

    private List<TmTeamRow> fetchTeams() {
        return tmJdbcTemplate.query("""
                SELECT id, team_name, team_description, status
                FROM technician_teams
                """, (rs, row) -> new TmTeamRow(
                rs.getLong("id"),
                rs.getString("team_name"),
                rs.getString("team_description"),
                rs.getString("status")));
    }

    private Map<Long, List<TmMemberRow>> fetchMembersByTeam() {
        return tmJdbcTemplate.query("""
                SELECT team_id, technician_id, team_leader
                FROM technician_team_members
                """, (rs, row) -> new TmMemberRow(
                rs.getLong("team_id"),
                rs.getLong("technician_id"),
                rs.getBoolean("team_leader")))
                .stream()
                .collect(Collectors.groupingBy(TmMemberRow::teamId));
    }

    // ---------- Upsert into EAM ----------

    private Map<Long, Technician> upsertTechnicians(List<TmUserRow> rows) {
        Map<Long, Technician> result = new HashMap<>();
        for (TmUserRow row : rows) {
            String externalId = externalTechId(row.id());

            Optional<Technician> existingById = technicianRepository
                    .findFirstByTechnicianIdIgnoreCase(externalId);
            Optional<Technician> existingByEmail = row.email() == null
                    ? Optional.empty()
                    : technicianRepository.findFirstByEmailIgnoreCase(row.email());

            Technician tech = existingById.or(() -> existingByEmail).orElseGet(Technician::new);

            tech.setTechnicianId(externalId);
            if (tech.getBadgeNumber() == null || tech.getBadgeNumber().isBlank()) {
                tech.setBadgeNumber("TM-BADGE-" + row.id());
            }
            tech.setFirstName(defaulted(row.firstName(), "Technician"));
            tech.setLastName(defaulted(row.lastName(), String.valueOf(row.id())));
            tech.setEmail(row.email());
            tech.setTechnicianType(TechnicianType.FULL_TIME);
            tech.setStatus(row.active() ? TechnicianStatus.ACTIVE : TechnicianStatus.INACTIVE);
            tech.setDeleted(!row.active());

            Technician saved = technicianRepository.save(tech);
            result.put(row.id(), saved);
        }
        return result;
    }

    private void upsertTeams(List<TmTeamRow> teams,
                             Map<Long, List<TmMemberRow>> members,
                             Map<Long, Technician> tmIdToTech) {

        Map<String, TechnicianTeam> existingByName = technicianTeamRepository.findAll().stream()
                .filter(t -> t.getTeamName() != null && !t.getTeamName().isBlank())
                .collect(Collectors.toMap(t -> t.getTeamName().toLowerCase(Locale.ROOT),
                        Function.identity(), (a, b) -> a));

        for (TmTeamRow row : teams) {
            if (row.name() == null || row.name().isBlank()) {
                continue;
            }
            String key = row.name().toLowerCase(Locale.ROOT);
            TechnicianTeam team = existingByName.getOrDefault(key, new TechnicianTeam());
            team.setTeamName(row.name());
            team.setTeamDescription(row.description());
            team.setStatus(parseStatus(row.status()));

            TechnicianTeam saved = technicianTeamRepository.save(team);
            syncMembers(saved, members.getOrDefault(row.id(), List.of()), tmIdToTech);
        }
    }

    private void syncMembers(TechnicianTeam team,
                             List<TmMemberRow> desired,
                             Map<Long, Technician> tmIdToTech) {

        Map<Long, TechnicianTeamMember> current = technicianTeamMemberRepository.findByTeam_Id(team.getId())
                .stream()
                .collect(Collectors.toMap(m -> m.getTechnician().getId(), Function.identity()));

        Set<Long> keep = new HashSet<>();
        for (TmMemberRow row : desired) {
            Technician tech = tmIdToTech.get(row.technicianId());
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

    private String defaulted(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }

    // ---------- DTOs ----------
    public record SyncReport(int technicians, int teams) {}
    private record TmUserRow(Long id, String firstName, String lastName, String email, boolean active) {}
    private record TmTeamRow(Long id, String name, String description, String status) {}
    private record TmMemberRow(Long teamId, Long technicianId, boolean teamLeader) {}
}
