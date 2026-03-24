package com.example.eam.Maintenance.Preventive.Entity;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Enum.MeterType;
import com.example.eam.Enum.PreventiveScheduleType;
import com.example.eam.Enum.PriorityLevel;
import com.example.eam.Enum.TimeFrequencyUnit;
import com.example.eam.Enum.WorkType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pm_plans", indexes = {
        @Index(name = "idx_pm_plan_code", columnList = "plan_code", unique = true),
        @Index(name = "idx_pm_plan_asset", columnList = "asset_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreventivePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "plan_code", nullable = false, unique = true, length = 64)
    private String planCode;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @Column(name = "location", length = 255)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_type", nullable = false, length = 50)
    private WorkType workType;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 32)
    private PriorityLevel priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 32)
    private PreventiveScheduleType scheduleType;

    @Column(name = "lead_time_days", nullable = false)
    private Integer leadTimeDays;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "interval_unit", length = 16)
    private TimeFrequencyUnit intervalUnit; // required for TIME_BASED

    @Column(name = "interval_value")
    private Integer intervalValue;          // required for TIME_BASED

    @Enumerated(EnumType.STRING)
    @Column(name = "meter_type", length = 32)
    private MeterType meterType;            // required for USAGE_BASED

    @Column(name = "meter_interval_value")
    private Integer meterIntervalValue;     // required for USAGE_BASED

    @Column(name = "current_meter_reading")
    private Integer currentMeterReading;    // optional initial meter

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;          // used for TIME_BASED

    @Column(name = "next_due_meter")
    private Integer nextDueMeter;           // used for USAGE_BASED

    @Column(name = "last_generated_due_date")
    private LocalDate lastGeneratedDueDate;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
