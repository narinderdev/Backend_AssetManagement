package com.example.eam.Procurement.Entity;

import com.example.eam.Procurement.Enum.MaterialRequisitionStatus;
import com.example.eam.Procurement.Enum.MaterialRequisitionShipToType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "material_requisitions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_mr_number", columnNames = "mr_number")
        },
        indexes = {
                @Index(name = "idx_mr_status", columnList = "status"),
                @Index(name = "idx_mr_ship_to_type", columnList = "ship_to_type")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialRequisition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mr_number", nullable = false, unique = true, length = 64)
    private String mrNumber;

    @Column(name = "requested_by_user_id", nullable = false, length = 150)
    private String requestedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private MaterialRequisitionStatus status = MaterialRequisitionStatus.DRAFT;

    @Column(name = "needed_by_date")
    private LocalDate neededByDate;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "department", length = 255)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(name = "ship_to_type", length = 20)
    private MaterialRequisitionShipToType shipToType;

    @Column(name = "ship_to_warehouse_id")
    private Long shipToWarehouseId;

    @Column(name = "ship_to_work_order_id")
    private Long shipToWorkOrderId;

    @Column(name = "approved_by_user_id", length = 150)
    private String approvedByUserId;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejected_by_user_id", length = 150)
    private String rejectedByUserId;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "materialRequisition", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MaterialRequisitionLine> lines = new ArrayList<>();

    public void addLine(MaterialRequisitionLine line) {
        line.setMaterialRequisition(this);
        this.lines.add(line);
    }
}
