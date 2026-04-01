package com.example.eam.Maintenance.Iot.Entity;

import com.example.eam.Asset.Entity.Asset;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "iot_devices", indexes = {
        @Index(name = "idx_iot_device_company", columnList = "company_id"),
        @Index(name = "idx_iot_device_asset", columnList = "asset_id"),
        @Index(name = "ux_iot_device_company_uid", columnList = "company_id, device_uid", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IotDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "device_uid", nullable = false, length = 120)
    private String deviceUid;

    @Column(name = "device_name", nullable = false, length = 160)
    private String deviceName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "auth_token_hash", nullable = false, length = 255)
    private String authTokenHash;

    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
