package com.example.eam.Maintenance.Emergency.Entity;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.WorkOrder.Entity.WorkOrder;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_incidents", indexes = {
        @Index(name = "idx_emergency_incident_asset", columnList = "asset_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id")
    private Long companyId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false, unique = true)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @Column(name = "location", length = 255)
    private String location;

    @Lob
    @Column(name = "failure_description", nullable = false)
    private String failureDescription;

    @Column(name = "failure_time", nullable = false)
    private LocalDateTime failureTime;

    @Column(name = "downtime_start")
    private LocalDateTime downtimeStart;

    @Column(name = "downtime_end")
    private LocalDateTime downtimeEnd;

    @Column(name = "reporter", length = 120)
    private String reporter;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
