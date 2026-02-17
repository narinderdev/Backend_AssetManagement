package com.example.eam.Security.Entity;

import com.example.eam.Enum.SecurityEventCategory;
import com.example.eam.Enum.SecurityEventResult;
import com.example.eam.Enum.SecurityEventType;
import com.example.eam.Enum.SecurityTargetType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_events", indexes = {
        @Index(name = "idx_security_event_type", columnList = "event_type"),
        @Index(name = "idx_security_event_category", columnList = "category"),
        @Index(name = "idx_security_event_target_type", columnList = "target_type"),
        @Index(name = "idx_security_event_created_at", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private SecurityEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private SecurityEventCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 32)
    private SecurityTargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "target_name", length = 255)
    private String targetName;

    @Column(name = "performed_by_id")
    private Long performedById;

    @Column(name = "performed_by", length = 255)
    private String performedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 16)
    private SecurityEventResult result;

    @Column(name = "details", columnDefinition = "NVARCHAR(MAX)")
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
