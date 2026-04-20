package com.example.eam.Iot.Entity;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Enum.IotAlertSeverity;
import com.example.eam.Enum.IotAlertStatus;
import com.example.eam.Enum.IotAnomalyType;
import com.example.eam.ServiceMaintenance.Entity.ServiceMaintenance;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "iot_alerts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IotAlert {

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
    @JoinColumn(name = "rule_id")
    private IotAlertRule rule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_id")
    private IotMetricCatalog metric;

    @Column(name = "metric_code", nullable = false, length = 64)
    private String metricCode;

    @Column(name = "latest_value")
    private Double latestValue;

    @Column(name = "threshold_value")
    private Double thresholdValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 16)
    private IotAlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "anomaly_type", nullable = false, length = 32)
    private IotAnomalyType anomalyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IotAlertStatus status;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "last_triggered_at", nullable = false)
    private LocalDateTime lastTriggeredAt;

    @Column(name = "last_normal_at")
    private LocalDateTime lastNormalAt;

    @Column(name = "healthy_streak", nullable = false)
    private Integer healthyStreak;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_service_request_id")
    private ServiceMaintenance linkedServiceRequest;

    @Column(name = "acknowledged_by", length = 255)
    private String acknowledgedBy;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolved_by", length = 255)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "suppressed_until")
    private LocalDateTime suppressedUntil;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

