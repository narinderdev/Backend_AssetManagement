package com.example.eam.Maintenance.Predictive.Entity;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Enum.MeterType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "predictive_meter_readings", indexes = {
        @Index(name = "idx_pm_readings_threshold", columnList = "threshold_id"),
        @Index(name = "idx_pm_readings_asset", columnList = "asset_id"),
        @Index(name = "idx_pm_readings_meter_time", columnList = "meter_type, reading_time")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictiveMeterReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "threshold_id", nullable = false)
    private AssetThreshold threshold;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @Enumerated(EnumType.STRING)
    @Column(name = "meter_type", nullable = false, length = 32)
    private MeterType meterType;

    @Column(name = "reading_value", nullable = false)
    private Double readingValue;

    @Column(name = "reading_time", nullable = false)
    private LocalDateTime readingTime;

    @Column(name = "severity", length = 16)
    private String severity;

    @Lob
    @Column(name = "notes")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
