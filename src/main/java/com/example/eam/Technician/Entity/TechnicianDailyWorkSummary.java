package com.example.eam.Technician.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "technician_daily_work_summaries",
        uniqueConstraints = @UniqueConstraint(name = "uk_technician_daily_work", columnNames = {"technician_id", "work_date"}),
        indexes = {
                @Index(name = "idx_tech_daily_work_tech", columnList = "technician_id"),
                @Index(name = "idx_tech_daily_work_date", columnList = "work_date")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianDailyWorkSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technician_id", nullable = false)
    private Technician technician;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Builder.Default
    @Column(name = "working_seconds", nullable = false)
    private Long workingSeconds = 0L;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
