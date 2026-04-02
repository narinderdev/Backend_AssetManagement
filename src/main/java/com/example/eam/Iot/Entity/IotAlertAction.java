package com.example.eam.Iot.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "iot_alert_actions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IotAlertAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    private IotAlert alert;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "action_type", nullable = false, length = 64)
    private String actionType;

    @Column(name = "action_by", length = 255)
    private String actionBy;

    @Column(name = "action_details", length = 1000)
    private String actionDetails;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

