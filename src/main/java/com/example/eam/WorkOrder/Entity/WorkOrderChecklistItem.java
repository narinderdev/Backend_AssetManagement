package com.example.eam.WorkOrder.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "work_order_checklist_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @Column(name = "item_text", nullable = false, length = 500)
    private String itemText;

    @Column(name = "required", nullable = false)
    private Boolean required;

    @Column(name = "completed", nullable = false)
    private Boolean completed;
}
