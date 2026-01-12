package com.example.eam.Maintenance.Preventive.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pm_plan_checklist_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreventivePlanChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pm_plan_id", nullable = false)
    private PreventivePlan plan;

    @Column(name = "item_text", nullable = false, length = 500)
    private String itemText;

    @Column(name = "required", nullable = false)
    private Boolean required;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
