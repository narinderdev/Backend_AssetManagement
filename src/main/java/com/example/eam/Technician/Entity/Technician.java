package com.example.eam.Technician.Entity;

import com.example.eam.Enum.TechnicianStatus;
import com.example.eam.Enum.TechnicianType;
import com.example.eam.TechnicianTeam.Entity.TechnicianTeamMember;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "technicians",
        uniqueConstraints = @UniqueConstraint(name = "uk_technicians_email", columnNames = "email"),
        indexes = {
                @Index(name = "idx_technicians_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Technician {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "full_name", length = 200)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "technician_type", nullable = false, length = 32)
    private TechnicianType technicianType;

    @Lob
    @Column(name = "skills")
    private String skills;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(name = "email", length = 100)
    private String email;

    @Lob
    @Column(name = "address")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TechnicianStatus status;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "work_shift", length = 64)
    private String workShift;

    @Lob
    @Column(name = "certifications")
    private String certifications;

    @Lob
    @Column(name = "notes")
    private String notes;

    @Builder.Default
    @OneToMany(mappedBy = "technician", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TechnicianTeamMember> teamMemberships = new ArrayList<>();

    @PrePersist
    @PreUpdate
    public void updateFullName() {
        String first = firstName != null ? firstName.trim() : "";
        String last = lastName != null ? lastName.trim() : "";
        String combined = (first + " " + last).trim();
        this.fullName = combined.isBlank() ? null : combined;
    }
}
