package com.example.eam.TechnicianTeam.Entity;

import com.example.eam.Enum.TechnicianTeamStatus;
import com.example.eam.TechnicianTeam.Entity.TechnicianTeamMember;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "technician_teams",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_technician_teams_team_name",
                columnNames = "team_name"
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "team_name", nullable = false, length = 100)
    private String teamName;

    @Lob
    @Column(name = "team_description")
    private String teamDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TechnicianTeamStatus status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Lob
    @Column(name = "notes")
    private String notes;

    @Builder.Default
    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TechnicianTeamMember> members = new ArrayList<>();
}
