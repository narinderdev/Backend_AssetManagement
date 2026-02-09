package com.example.eam.ServiceMaintenance.Entity;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Enum.MaintenanceType;
import com.example.eam.Enum.RequestPriority;
import com.example.eam.Enum.ServiceRequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "service_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceMaintenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                        // DB primary key

    @Column(name = "request_id", nullable = false, unique = true, length = 50)
    private String requestId;              // Business Request ID (SR-000001 etc.)

    @Column(name = "linked_work_order_id", length = 64)
    private String linkedWorkOrderId;

    @Column(name = "request_date", nullable = false)
    private LocalDateTime requestDate;      // Auto: current date-time

    @Column(name = "requester_name", nullable = false, length = 255)
    private String requesterName;

    @Column(name = "requester_contact", length = 255)
    private String requesterContact;

    @Column(name = "department", length = 255)
    private String department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;                    // Optional

    @Column(name = "location", length = 255)
    private String location;                // Auto from asset if not provided

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_type", nullable = false, length = 50)
    private MaintenanceType maintenanceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 50)
    private RequestPriority priority;

    @Column(name = "short_title", nullable = false, length = 255)
    private String shortTitle;

    @Column(name = "problem_description", nullable = false, length = 2000)
    private String problemDescription;

    @Column(name = "preferred_date_time")
    private LocalDateTime preferredDateTime;

    @Column(name = "preferred_technician_id")
    private Long preferredTechnicianId;

    @Column(name = "preferred_team_id")
    private Long preferredTeamId;

    @Column(name = "safety_risk")
    private Boolean safetyRisk;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ServiceRequestStatus status;

    @Column(name = "approved_by", length = 255)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason", length = 2000)
    private String rejectionReason;

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;
}
