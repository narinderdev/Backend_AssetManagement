package com.example.eam.Maintenance.Iot.Entity;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Enum.MeterType;
import com.example.eam.Maintenance.Iot.Enum.IotAlertSeverity;
import com.example.eam.Maintenance.Iot.Enum.IotAlertStatus;
import com.example.eam.Maintenance.Iot.Enum.IotAlertType;
import com.example.eam.ServiceMaintenance.Entity.ServiceMaintenance;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "iot_alerts", indexes = {
        @Index(name = "idx_iot_alert_company_status", columnList = "company_id, status, occurred_at"),
        @Index(name = "idx_iot_alert_device_meter", columnList = "device_id, meter_type, occurred_at"),
        @Index(name = "idx_iot_alert_asset", columnList = "asset_id")
})
@Data
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

    @Column(name = "threshold_value")
    private Double thresholdValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 16)
    private IotAlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 32)
    private IotAlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private IotAlertStatus status;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id")
    private ServiceMaintenance serviceRequest;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
