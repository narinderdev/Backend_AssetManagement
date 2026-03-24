package com.example.eam.InventoryManagement.Entity;

import com.example.eam.Enum.InventoryReconciliationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_reconciliations", indexes = {
        @Index(name = "idx_invrec_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_invrec_item", columnList = "inventory_item_id"),
        @Index(name = "idx_invrec_status", columnList = "status"),
        @Index(name = "idx_invrec_date", columnList = "reconcile_date")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReconciliation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Reconciliation ID

    @Column(name = "company_id")
    private Long companyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @Column(name = "reconcile_date", nullable = false)
    private LocalDate reconcileDate;

    @Column(name = "entered_by", length = 150)
    private String enteredBy;

    @Column(name = "system_quantity", nullable = false)
    private Integer systemQuantity;

    @Column(name = "physical_quantity", nullable = false)
    private Integer physicalQuantity;

    @Column(name = "variance_quantity", nullable = false)
    private Integer varianceQuantity;

    @Column(name = "cost_per_unit_snapshot", precision = 19, scale = 2)
    private BigDecimal costPerUnitSnapshot;

    @Column(name = "variance_cost", precision = 19, scale = 2)
    private BigDecimal varianceCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private InventoryReconciliationStatus status;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "approved_by", length = 150)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approval_comment", length = 500)
    private String approvalComment;

    @Column(name = "rejected_by", length = 150)
    private String rejectedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_comment", length = 500)
    private String rejectionComment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
