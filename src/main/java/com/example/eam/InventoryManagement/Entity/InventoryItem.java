package com.example.eam.InventoryManagement.Entity;

import com.example.eam.Enum.UnitOfMeasure;
import com.example.eam.VendorManagement.Entity.Vendor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "inventory_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inventory_items_item_id", columnNames = "item_id")
        },
        indexes = {
                @Index(name = "idx_inventory_items_deleted", columnList = "deleted"),
                @Index(name = "idx_inventory_items_category", columnList = "category")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // DB PK

    @Column(name = "item_id", nullable = false, unique = true, length = 64)
    private String itemId; // Business ID

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @Column(name = "category", nullable = false, length = 128)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "uom", nullable = false, length = 32)
    private UnitOfMeasure unitOfMeasure;

    @Column(name = "manufacturer", length = 255)
    private String manufacturer;

    @Column(name = "manufacturer_part_number", length = 255)
    private String manufacturerPartNumber;

    @Column(name = "stock_level", nullable = false)
    private Integer stockLevel;

    @Column(name = "reorder_point", nullable = false)
    private Integer reorderPoint;

    @Column(name = "reorder_quantity", nullable = false)
    private Integer reorderQuantity;

    @Column(name = "cost_per_unit", precision = 19, scale = 2)
    private BigDecimal costPerUnit;

    @Column(name = "min_stock_level")
    private Integer minStockLevel;

    @Column(name = "max_stock_level")
    private Integer maxStockLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_vendor_id")
    private Vendor primaryVendor;

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

