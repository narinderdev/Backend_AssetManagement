package com.example.eam.Procurement.Entity;

import com.example.eam.Procurement.Enum.PurchaseOrderShipToType;
import com.example.eam.Procurement.Enum.PurchaseOrderStatus;
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
        name = "purchase_orders",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_po_number", columnNames = "po_number")
        },
        indexes = {
                @Index(name = "idx_po_status", columnList = "status"),
                @Index(name = "idx_po_vendor", columnList = "vendor_id"),
                @Index(name = "idx_po_mr", columnList = "mr_id"),
                @Index(name = "idx_po_ship_to_type", columnList = "ship_to_type")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "po_number", nullable = false, unique = true, length = 64)
    private String poNumber;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Column(name = "mr_id")
    private Long mrId;

    @Column(name = "department", length = 255)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(name = "ship_to_type", length = 20)
    private PurchaseOrderShipToType shipToType;

    @Column(name = "ship_to_warehouse_id")
    private Long shipToWarehouseId;

    @Column(name = "ship_to_work_order_id")
    private Long shipToWorkOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PurchaseOrderStatus status = PurchaseOrderStatus.ISSUED;

    // Keep column name for backward compatibility; semantic name is now "required delivery date".
    @Column(name = "expected_delivery_date")
    private LocalDate requiredDeliveryDate;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "gl_account_string", length = 255)
    private String glAccountString;

    @Column(name = "created_by_user_id", nullable = false, length = 150)
    private String createdByUserId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "delivered_at", columnDefinition = "datetimeoffset")
    private Instant deliveredAt;

    @OneToMany(mappedBy = "po", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PurchaseOrderLine> lines = new ArrayList<>();

    public void addLine(PurchaseOrderLine line) {
        line.setPo(this);
        this.lines.add(line);
    }
}
