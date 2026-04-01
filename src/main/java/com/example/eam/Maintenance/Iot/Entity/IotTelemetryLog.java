package com.example.eam.Maintenance.Iot.Entity;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Enum.MeterType;
import com.example.eam.Maintenance.Iot.Enum.IotAlertSeverity;
import com.example.eam.Maintenance.Iot.Enum.IotAlertType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "iot_telemetry_logs", indexes = {
        @Index(name = "idx_iot_telemetry_company_time", columnList = "company_id, reading_time"),
        @Index(name = "idx_iot_telemetry_device_meter", columnList = "device_id, meter_type, reading_time"),
        @Index(name = "idx_iot_telemetry_asset", columnList = "asset_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IotTelemetryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private IotDevice device;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 16)
    private IotAlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "anomaly_type", length = 32)
    private IotAlertType anomalyType;

    @Lob
    @Column(name = "notes")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
