package com.example.eam.InventoryManagement.Entity;

import com.example.eam.Enum.InventoryReferenceType;
import com.example.eam.Enum.InventoryTransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_audit_logs", indexes = {
        @Index(name = "idx_inv_audit_item", columnList = "inventory_item_id"),
        @Index(name = "idx_inv_audit_txn_type", columnList = "transaction_type"),
        @Index(name = "idx_inv_audit_ref_type", columnList = "reference_type"),
        @Index(name = "idx_inv_audit_created", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 16)
    private InventoryTransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 32)
    private InventoryReferenceType referenceType;

    @Column(name = "reference_number", length = 128)
    private String referenceNumber;

    @Column(name = "before_quantity", nullable = false)
    private Integer beforeQuantity;

    @Column(name = "after_quantity", nullable = false)
    private Integer afterQuantity;

    @Column(name = "variance_quantity", nullable = false)
    private Integer varianceQuantity;

    @Column(name = "performed_by", length = 150)
    private String performedBy;

    @Column(name = "reason", length = 500)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
