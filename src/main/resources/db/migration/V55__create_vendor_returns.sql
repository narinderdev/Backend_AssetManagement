CREATE TABLE vendor_returns (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    grn_id BIGINT NOT NULL,
    grn_line_id BIGINT NOT NULL,
    po_id BIGINT NULL,
    po_line_id BIGINT NULL,
    vendor_id BIGINT NULL,
    item_id BIGINT NOT NULL,
    return_qty DECIMAL(19,4) NOT NULL,
    unit_cost_snapshot DECIMAL(19,4) NULL,
    return_cost DECIMAL(19,2) NULL,
    stock_before INT NULL,
    stock_after INT NULL,
    reason NVARCHAR(500) NULL,
    performed_by NVARCHAR(150) NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT df_vendor_returns_created DEFAULT SYSUTCDATETIME()
);

CREATE INDEX idx_vendor_returns_grn ON vendor_returns(grn_id);
CREATE INDEX idx_vendor_returns_vendor ON vendor_returns(vendor_id);
CREATE INDEX idx_vendor_returns_item ON vendor_returns(item_id);

ALTER TABLE vendor_returns
    ADD CONSTRAINT fk_vendor_returns_grn FOREIGN KEY (grn_id) REFERENCES goods_receipt_notes(id);

ALTER TABLE vendor_returns
    ADD CONSTRAINT fk_vendor_returns_grn_line FOREIGN KEY (grn_line_id) REFERENCES goods_receipt_note_lines(id);

ALTER TABLE vendor_returns
    ADD CONSTRAINT fk_vendor_returns_item FOREIGN KEY (item_id) REFERENCES inventory_items(id);

ALTER TABLE vendor_returns
    ADD CONSTRAINT fk_vendor_returns_po FOREIGN KEY (po_id) REFERENCES purchase_orders(id);

ALTER TABLE vendor_returns
    ADD CONSTRAINT fk_vendor_returns_po_line FOREIGN KEY (po_line_id) REFERENCES purchase_order_lines(id);
