package com.example.eam.WorkOrder.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "wo_number_pool",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_wo_number_pool_wo_number", columnNames = "wo_number")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WoNumberPool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wo_number", nullable = false, length = 50)
    private String woNumber;

    @Builder.Default
    @Column(name = "is_assigned", nullable = false)
    private boolean assigned = false;

    @Column(name = "assigned_to_wo_id")
    private Long assignedToWoId;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
