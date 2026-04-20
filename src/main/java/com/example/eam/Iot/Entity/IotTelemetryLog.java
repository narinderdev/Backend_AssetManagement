package com.example.eam.Iot.Entity;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Enum.IngestProcessingStatus;
import com.example.eam.Enum.IotAlertSeverity;
import com.example.eam.Enum.IotAnomalyType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "iot_telemetry_logs")
@Getter
@Setter
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
    @JoinColumn(name = "device_id")
    private IotDevice device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_id")
    private IotMetricCatalog metric;

    @Column(name = "metric_code", nullable = false, length = 64)
    private String metricCode;

    @Column(name = "event_id", length = 128)
    private String eventId;

    @Column(name = "observed_at", nullable = false)
    private LocalDateTime observedAt;

    @Column(name = "reading_value", nullable = false)
    private Double readingValue;

    @Column(name = "location", length = 255)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "ingest_status", nullable = false, length = 16)
    private IngestProcessingStatus ingestStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_severity", length = 16)
    private IotAlertSeverity alertSeverity;

    @Enumerated(EnumType.STRING)
    @Column(name = "anomaly_type", length = 32)
    private IotAnomalyType anomalyType;

    @Column(name = "processing_attempts", nullable = false)
    private Integer processingAttempts;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

