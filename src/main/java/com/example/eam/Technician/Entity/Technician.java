package com.example.eam.Technician.Entity;

import com.example.eam.Enum.TechnicianStatus;
import com.example.eam.Enum.TechnicianType;
import com.example.eam.TechnicianTeam.Entity.TechnicianTeamMember;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "technicians",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_technicians_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_technicians_badge", columnNames = "badge_number"),
                @UniqueConstraint(name = "uk_technicians_identifier", columnNames = "technician_id")
        },
        indexes = {
                @Index(name = "idx_technicians_status", columnList = "status"),
                @Index(name = "idx_technicians_badge", columnList = "badge_number"),
                @Index(name = "idx_technicians_identifier", columnList = "technician_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE technicians SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Technician {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "technician_id", nullable = false, length = 64)
    private String technicianId;

    @Column(name = "badge_number", nullable = false, length = 64)
    private String badgeNumber;

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

    @Column(name = "technician_photo_url", length = 512)
    private String technicianPhotoUrl;

    @Column(name = "certificate_url", length = 512)
    private String certificateUrl;

    @Column(name = "certificate_issue_date")
    private LocalDate certificateIssueDate;

    @Column(name = "certificate_expiry_date")
    private LocalDate certificateExpiryDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Lob
    @Column(name = "certifications")
    private String certifications;

    @Lob
    @Column(name = "notes")
    private String notes;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

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
