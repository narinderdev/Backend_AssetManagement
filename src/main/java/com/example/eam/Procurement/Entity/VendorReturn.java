package com.example.eam.Procurement.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "vendor_returns",
        indexes = {
                @Index(name = "idx_vendor_return_grn", columnList = "grn_id"),
                @Index(name = "idx_vendor_return_vendor", columnList = "vendor_id"),
                @Index(name = "idx_vendor_return_item", columnList = "item_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "grn_id", nullable = false)
    private Long grnId;

    @Column(name = "grn_line_id", nullable = false)
    private Long grnLineId;

    @Column(name = "po_id")
    private Long poId;

    @Column(name = "po_line_id")
    private Long poLineId;

    @Column(name = "vendor_id")
    private Long vendorId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "return_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal returnQty;

    @Column(name = "unit_cost_snapshot", precision = 19, scale = 4)
    private BigDecimal unitCostSnapshot;

    @Column(name = "return_cost", precision = 19, scale = 2)
    private BigDecimal returnCost;

    @Column(name = "stock_before")
    private Integer stockBefore;

    @Column(name = "stock_after")
    private Integer stockAfter;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "performed_by", length = 150)
    private String performedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
