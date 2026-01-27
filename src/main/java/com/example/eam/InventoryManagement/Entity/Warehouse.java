package com.example.eam.InventoryManagement.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "warehouses",
        uniqueConstraints = @UniqueConstraint(name = "ux_warehouses_name", columnNames = "name"),
        indexes = {
                @Index(name = "idx_warehouses_deleted", columnList = "deleted")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "address", length = 512)
    private String address;

    @Column(name = "zone_aisle", length = 128)
    private String zoneAisle;

    @Column(name = "rack_shelf", length = 128)
    private String rackShelf;

    @Column(name = "bin_code", length = 128)
    private String binCode;

    @Column(name = "bin_description", length = 512)
    private String binDescription;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
