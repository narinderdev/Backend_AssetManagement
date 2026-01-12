package com.example.eam.Maintenance.Predictive.Entity;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Enum.MeterType;
import com.example.eam.Enum.PriorityLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "asset_thresholds", indexes = {
        @Index(name = "idx_asset_threshold_asset_meter", columnList = "asset_id,meter_type", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Enumerated(EnumType.STRING)
    @Column(name = "meter_type", nullable = false, length = 32)
    private MeterType meterType;

    @Column(name = "warning_threshold")
    private Double warningThreshold;

    @Column(name = "critical_threshold")
    private Double criticalThreshold;

    @Column(name = "auto_create_wo", nullable = false)
    private Boolean autoCreateWo;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_priority", length = 32)
    private PriorityLevel defaultPriority;

    @Column(name = "cooldown_hours")
    private Integer cooldownHours;

    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;

    @Column(name = "last_triggered_severity", length = 16)
    private String lastTriggeredSeverity;
}
