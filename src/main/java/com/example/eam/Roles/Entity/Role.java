package com.example.eam.Roles.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
    name = "roles",
    uniqueConstraints = @UniqueConstraint(name = "uk_role_name", columnNames = "name"),
    indexes = {
        @Index(name = "idx_role_active", columnList = "active"),
        @Index(name = "idx_role_name", columnList = "name")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g. "Planner", "Technician", "Admin"
    @Column(name = "name", nullable = false, unique = true, length = 80)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(name = "technician_role", nullable = false)
    private boolean technicianRole = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id"),
        uniqueConstraints = @UniqueConstraint(
            name = "uk_role_permission",
            columnNames = {"role_id", "permission_id"}
        )
    )
    @Builder.Default
    private Set<AppPermission> permissions = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

