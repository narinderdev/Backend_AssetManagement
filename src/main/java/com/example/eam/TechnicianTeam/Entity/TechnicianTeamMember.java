package com.example.eam.TechnicianTeam.Entity;

import com.example.eam.Technician.Entity.Technician;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "technician_team_members",
        uniqueConstraints = @UniqueConstraint(name = "uk_technician_team_members_team_technician", columnNames = {"team_id", "technician_id"}),
        indexes = {
                @Index(name = "idx_technician_team_members_team_id", columnList = "team_id"),
                @Index(name = "idx_technician_team_members_technician_id", columnList = "technician_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianTeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private TechnicianTeam team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technician_id", nullable = false)
    private Technician technician;

    @Builder.Default
    @Column(name = "team_leader", nullable = false)
    private boolean teamLeader = false;
}
