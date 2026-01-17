package com.example.eam.WorkOrder.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "work_order_pause_logs",
        indexes = {
                @Index(name = "idx_wo_pause_check_log", columnList = "check_log_id")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderPauseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_log_id", nullable = false)
    private WorkOrderCheckLog checkLog;

    @Column(name = "pause_at", nullable = false)
    private LocalDateTime pauseAt;

    @Column(name = "resume_at")
    private LocalDateTime resumeAt;
}
