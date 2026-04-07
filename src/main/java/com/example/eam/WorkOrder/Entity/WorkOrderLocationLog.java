package com.example.eam.WorkOrder.Entity;

import com.example.eam.Enum.WorkOrderGeofenceEventType;
import com.example.eam.Enum.WorkOrderStatus;
import com.example.eam.Technician.Entity.Technician;
import com.example.eam.TechnicianTeam.Entity.TechnicianTeam;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_order_location_logs",
        indexes = {
                @Index(name = "idx_wo_loc_logs_work_order_observed", columnList = "work_order_id, observed_at"),
                @Index(name = "idx_wo_loc_logs_technician_observed", columnList = "technician_id, observed_at"),
                @Index(name = "idx_wo_loc_logs_observed", columnList = "observed_at")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderLocationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id")
    private Long companyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private Technician technician;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private TechnicianTeam team;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "observed_at", nullable = false)
    private LocalDateTime observedAt;

    @Column(name = "distance_meters", precision = 10, scale = 2)
    private BigDecimal distanceMeters;

    @Column(name = "geofence_radius_meters", nullable = false)
    private Integer geofenceRadiusMeters;

    @Column(name = "inside_geofence", nullable = false)
    private boolean insideGeofence;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private WorkOrderGeofenceEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_before", length = 32)
    private WorkOrderStatus statusBefore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_after", length = 32)
    private WorkOrderStatus statusAfter;

    @Column(name = "auto_check_in", nullable = false)
    private boolean autoCheckIn;

    @Column(name = "auto_check_out", nullable = false)
    private boolean autoCheckOut;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
