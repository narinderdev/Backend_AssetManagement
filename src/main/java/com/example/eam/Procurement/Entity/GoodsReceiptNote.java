package com.example.eam.Procurement.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "goods_receipt_notes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_grn_number", columnNames = "grn_number")
        },
        indexes = {
                @Index(name = "idx_grn_po_id", columnList = "po_id"),
                @Index(name = "idx_grn_vendor_id", columnList = "vendor_id"),
                @Index(name = "idx_grn_day_key", columnList = "day_key_utc")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceiptNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "grn_number", nullable = false, unique = true, length = 64)
    private String grnNumber;

    @Column(name = "po_id")
    private Long poId;

    @Column(name = "vendor_id")
    private Long vendorId;

    @Column(name = "received_by_user_id", nullable = false, length = 150)
    private String receivedByUserId;

    @Column(name = "received_at_utc", nullable = false)
    private Instant receivedAtUtc;

    @Column(name = "day_key_utc", nullable = false, length = 16)
    private String dayKeyUtc;

    @Column(name = "notes", length = 1000)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "grn", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GoodsReceiptNoteLine> lines = new ArrayList<>();

    public void addLine(GoodsReceiptNoteLine line) {
        line.setGrn(this);
        this.lines.add(line);
    }
}
