package com.example.eam.VoiceAI.Entity;

import com.example.eam.Enum.MaintenanceType;
import com.example.eam.Enum.RequestPriority;
import com.example.eam.ServiceMaintenance.Entity.ServiceMaintenance;
import com.example.eam.VoiceAI.Enum.VoiceCallIntent;
import com.example.eam.VoiceAI.Enum.VoiceIntakeOutcome;
import com.example.eam.WorkOrder.Entity.WorkOrder;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "voice_ai_intakes",
        indexes = {
                @Index(name = "idx_voice_ai_intakes_company_created", columnList = "company_id, created_at"),
                @Index(name = "idx_voice_ai_intakes_service_request", columnList = "service_request_id"),
                @Index(name = "idx_voice_ai_intakes_work_order", columnList = "work_order_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceAiIntake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "external_call_id", length = 128)
    private String externalCallId;

    @Enumerated(EnumType.STRING)
    @Column(name = "intent", nullable = false, length = 64)
    private VoiceCallIntent intent;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 64)
    private VoiceIntakeOutcome outcome;

    @Column(name = "requester_name", length = 255)
    private String requesterName;

    @Column(name = "requester_phone_number", length = 32)
    private String requesterPhoneNumber;

    @Column(name = "requester_contact", length = 255)
    private String requesterContact;

    @Column(name = "department", length = 255)
    private String department;

    @Column(name = "issue_description", length = 2000)
    private String issueDescription;

    @Column(name = "short_title", length = 255)
    private String shortTitle;

    @Column(name = "asset_db_id")
    private Long assetDbId;

    @Column(name = "asset_external_id", length = 128)
    private String assetExternalId;

    @Column(name = "asset_name", length = 255)
    private String assetName;

    @Column(name = "location", length = 255)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_priority", length = 32)
    private RequestPriority requestPriority;

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_type", length = 32)
    private MaintenanceType maintenanceType;

    @Lob
    @Column(name = "transcript")
    private String transcript;

    @Lob
    @Column(name = "structured_summary")
    private String structuredSummary;

    @Lob
    @Column(name = "skill_tags")
    private String skillTags;

    @Builder.Default
    @Column(name = "requires_escalation", nullable = false)
    private boolean requiresEscalation = false;

    @Column(name = "escalation_reason", length = 1000)
    private String escalationReason;

    @Column(name = "assigned_technician_id")
    private Long assignedTechnicianId;

    @Column(name = "assigned_technician_name", length = 255)
    private String assignedTechnicianName;

    @Column(name = "expected_response_at")
    private LocalDateTime expectedResponseAt;

    @Column(name = "customer_confirmation_message", length = 1000)
    private String customerConfirmationMessage;

    @Lob
    @Column(name = "status_snapshot")
    private String statusSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id")
    private ServiceMaintenance serviceRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id")
    private WorkOrder workOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
