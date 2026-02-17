package com.example.eam.WorkOrder.Entity;

import com.example.eam.Enum.CostTreatment;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "work_order_types",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_work_order_types_type", columnNames = "work_order_type")
        },
        indexes = {
                @Index(name = "idx_work_order_types_active", columnList = "active")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderTypeTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_order_type", nullable = false, length = 128)
    private String workOrderType;

    @Column(name = "default_gl_account", length = 255)
    private String defaultGlAccount;

    @Column(name = "default_utility_account", length = 255)
    private String defaultUtilityAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "cost_treatment", nullable = false, length = 16)
    private CostTreatment costTreatment;

    @Column(name = "labor_gl_account", length = 255)
    private String laborGlAccount;

    @Column(name = "labor_utility_account", length = 255)
    private String laborUtilityAccount;

    @Column(name = "inventory_gl_account", length = 255)
    private String inventoryGlAccount;

    @Column(name = "inventory_utility_account", length = 255)
    private String inventoryUtilityAccount;

    @Builder.Default
    @Column(name = "create_asset", nullable = false)
    private boolean createAsset = false;

    @Column(name = "property_unit", length = 128)
    private String propertyUnit;

    @Column(name = "property_group", length = 128)
    private String propertyGroup;

    @Column(name = "retirement_unit", length = 128)
    private String retirementUnit;

    @Column(name = "functional_class", length = 128)
    private String functionalClass;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
