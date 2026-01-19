package com.example.eam.Technician.Entity;

import com.example.eam.Enum.DevicePlatform;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "technician_device_tokens",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tech_device_token_token", columnNames = "device_token")
        },
        indexes = {
                @Index(name = "idx_tech_device_token_tech", columnList = "technician_id")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianDeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technician_id", nullable = false)
    private Technician technician;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", length = 16)
    private DevicePlatform platform;

    @Column(name = "device_token", nullable = false, length = 512)
    private String deviceToken;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
