package com.example.eam.Iot.Entity;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Enum.IotRuleOperator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "iot_alert_rules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IotAlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_id", nullable = false)
    private IotMetricCatalog metric;

    @Column(name = "location", length = 255)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_operator", nullable = false, length = 16)
    private IotRuleOperator ruleOperator;

    @Column(name = "low_threshold")
    private Double lowThreshold;

    @Column(name = "medium_threshold")
    private Double mediumThreshold;

    @Column(name = "high_threshold")
    private Double highThreshold;

    @Column(name = "critical_threshold")
    private Double criticalThreshold;

    @Column(name = "cooldown_minutes", nullable = false)
    private Integer cooldownMinutes;

    @Column(name = "spike_delta")
    private Double spikeDelta;

    @Column(name = "consecutive_abnormal_count")
    private Integer consecutiveAbnormalCount;

    @Column(name = "auto_create_service_request", nullable = false)
    private boolean autoCreateServiceRequest;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

