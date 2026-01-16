package com.example.eam.WorkOrder.Entity;

import com.example.eam.Technician.Entity.Technician;
import com.example.eam.TechnicianTeam.Entity.TechnicianTeam;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "work_order_check_logs",
        indexes = {
                @Index(name = "idx_wo_check_work_order", columnList = "work_order_id")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderCheckLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private Technician technician;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private TechnicianTeam team;

    @Column(name = "check_in_at", nullable = false)
    private LocalDateTime checkInAt;

    @Column(name = "check_out_at")
    private LocalDateTime checkOutAt;

    @Column(name = "pause_at")
    private LocalDateTime pauseAt;

    @Column(name = "resume_at")
    private LocalDateTime resumeAt;

    @Column(name = "notes", length = 500)
    private String notes;
}
