package com.example.eam.WorkOrder.Entity;

import com.example.eam.Technician.Entity.Technician;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "work_order_technician_daily_logs",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_wo_technician_daily",
                columnNames = {"work_order_id", "technician_id", "work_date"}
        ),
        indexes = {
                @Index(name = "idx_wo_tech_daily_work_order", columnList = "work_order_id"),
                @Index(name = "idx_wo_tech_daily_technician", columnList = "technician_id"),
                @Index(name = "idx_wo_tech_daily_date", columnList = "work_date")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderTechnicianDailyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technician_id", nullable = false)
    private Technician technician;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Builder.Default
    @Column(name = "working_seconds", nullable = false)
    private Long workingSeconds = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
