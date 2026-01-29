package com.example.eam.WorkOrder.Entity;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Enum.PriorityLevel;
import com.example.eam.Enum.WorkOrderSource;
import com.example.eam.Enum.WorkOrderStatus;
import com.example.eam.Enum.WorkType;
import com.example.eam.Maintenance.Preventive.Entity.PreventivePlan;
import com.example.eam.ServiceMaintenance.Entity.ServiceMaintenance;
import com.example.eam.WorkRequestType.Entity.WorkRequestType;
import com.example.eam.Technician.Entity.Technician;
import com.example.eam.TechnicianTeam.Entity.TechnicianTeam;
import com.example.eam.WorkOrder.Entity.WorkOrderTypeTemplate;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "work_orders",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_work_orders_work_order_id", columnNames = "work_order_id"),
        @UniqueConstraint(name = "uk_work_orders_work_order_number", columnNames = "work_order_number"),
        @UniqueConstraint(name = "uk_work_orders_service_request", columnNames = "service_request_id"),
        @UniqueConstraint(name = "uk_work_orders_pm_due", columnNames = {"pm_plan_id", "pm_due_date"})
    },
    indexes = {
        @Index(name = "idx_work_orders_asset_id", columnList = "asset_id"),
        @Index(name = "idx_work_orders_deleted", columnList = "deleted"),
        @Index(name = "idx_work_orders_status", columnList = "status"),
        @Index(name = "idx_work_orders_pm_plan", columnList = "pm_plan_id")
    }
)

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Business/display WO code (separate from DB id)
    @Column(name = "work_order_id", nullable = false, unique = true, length = 64)
    private String workOrderId;

    @Column(name = "work_order_number", unique = true, length = 50)
    private String woNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pm_plan_id")
    private PreventivePlan pmPlan;

        // Due date for this PM-generated Work Order (used to avoid duplicates)
   @Column(name = "pm_due_date")
   private LocalDate pmDueDate;
    

    // Optional link to Service Request (one request -> one WO)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id", unique = true)
    private ServiceMaintenance linkedRequest;

    // Asset is optional (allowed when WO is location-only)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_request_type_id")
    private WorkRequestType workRequestType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_type_id")
    private WorkOrderTypeTemplate workOrderTypeTemplate;

    // Snapshot of location (auto from asset if present, but editable)
    @Column(name = "location", length = 255)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_type", nullable = false, length = 32)
    private WorkType workType;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 16)
    private PriorityLevel priority;

    @Column(name = "wo_title", nullable = false, length = 255)
    private String woTitle;

    @Lob
    @Column(name = "description_scope")
    private String descriptionScope;

    // "User lookup" stored as string for now (later can be a User entity)
    @Column(name = "planner", length = 120)
    private String planner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_technician_id")
    private Technician assignedTechnician;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_team_id")
    private TechnicianTeam assignedTeam;

    @Column(name = "planned_start_datetime")
    private LocalDateTime plannedStartDateTime;

    @Column(name = "planned_end_datetime")
    private LocalDateTime plannedEndDateTime;

    @Column(name = "target_completion_date")
    private LocalDate targetCompletionDate;

    @Column(name = "actual_start_datetime")
    private LocalDateTime actualStartDateTime;

    @Column(name = "actual_end_datetime")
    private LocalDateTime actualEndDateTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private WorkOrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 32)
    private WorkOrderSource source;

    @Column(name = "estimated_labor_hours", precision = 10, scale = 2)
    private BigDecimal estimatedLaborHours;

    @Column(name = "estimated_material_cost", precision = 19, scale = 2)
    private BigDecimal estimatedMaterialCost;

    @Column(name = "estimated_total_cost", precision = 19, scale = 2)
    private BigDecimal estimatedTotalCost;

    @Column(name = "actual_labor_hours", precision = 10, scale = 2)
    private BigDecimal actualLaborHours;

    @Column(name = "actual_labor_cost", precision = 19, scale = 2)
    private BigDecimal actualLaborCost;

    @Column(name = "actual_material_cost", precision = 19, scale = 2)
    private BigDecimal actualMaterialCost;

    @Column(name = "actual_total_cost", precision = 19, scale = 2)
    private BigDecimal actualTotalCost;

    @Lob
    @Column(name = "completion_notes")
    private String completionNotes;

    @Lob
    @Column(name = "failure_cause")
    private String failureCause;

    @Lob
    @Column(name = "remedy_action")
    private String remedyAction;

    @Column(name = "before_photo_url", length = 512)
    private String beforePhotoUrl;

    @Column(name = "after_photo_url", length = 512)
    private String afterPhotoUrl;

    @Lob
    @Column(name = "supervisor_notes")
    private String supervisorNotes;

    @Lob
    @Column(name = "approval_notes")
    private String approvalNotes;

    @Lob
    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Lob
    @Column(name = "precheck_notes")
    private String precheckNotes;

    @Column(name = "gl_account", length = 255)
    private String glAccount;

    @Column(name = "utility_account", length = 255)
    private String utilityAccount;

    @Column(name = "labor_gl_account", length = 255)
    private String laborGlAccount;

    @Column(name = "labor_utility_account", length = 255)
    private String laborUtilityAccount;

    @Column(name = "inventory_gl_account", length = 255)
    private String inventoryGlAccount;

    @Column(name = "inventory_utility_account", length = 255)
    private String inventoryUtilityAccount;

    // Emergency / downtime tracking
    @Lob
    @Column(name = "failure_description")
    private String failureDescription;

    @Column(name = "failure_time")
    private LocalDateTime failureTime;

    @Column(name = "downtime_start")
    private LocalDateTime downtimeStart;

    @Column(name = "downtime_end")
    private LocalDateTime downtimeEnd;

    @Column(name = "reporter", length = 120)
    private String reporter;

    @Column(name = "approved_by", length = 120)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_by", length = 120)
    private String rejectedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

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
