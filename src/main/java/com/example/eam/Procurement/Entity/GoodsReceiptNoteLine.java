package com.example.eam.Procurement.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "goods_receipt_note_lines",
        indexes = {
                @Index(name = "idx_grn_line_grn", columnList = "grn_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceiptNoteLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grn_id", nullable = false)
    private GoodsReceiptNote grn;

    @Column(name = "po_line_id")
    private Long poLineId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "received_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal receivedQty;

    @Column(name = "return_qty", precision = 19, scale = 4)
    private BigDecimal returnQty;
}
