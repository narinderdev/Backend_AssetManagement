IF OBJECT_ID('dbo.vendor_returns', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.vendor_returns (
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
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_vendor_returns_grn' AND object_id = OBJECT_ID('dbo.vendor_returns'))
BEGIN
    CREATE INDEX idx_vendor_returns_grn ON dbo.vendor_returns(grn_id);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_vendor_returns_vendor' AND object_id = OBJECT_ID('dbo.vendor_returns'))
BEGIN
    CREATE INDEX idx_vendor_returns_vendor ON dbo.vendor_returns(vendor_id);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_vendor_returns_item' AND object_id = OBJECT_ID('dbo.vendor_returns'))
BEGIN
    CREATE INDEX idx_vendor_returns_item ON dbo.vendor_returns(item_id);
END;

IF OBJECT_ID('dbo.goods_receipt_notes', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_vendor_returns_grn' AND parent_object_id = OBJECT_ID('dbo.vendor_returns'))
BEGIN
    ALTER TABLE dbo.vendor_returns
        ADD CONSTRAINT fk_vendor_returns_grn FOREIGN KEY (grn_id) REFERENCES dbo.goods_receipt_notes(id);
END;

IF OBJECT_ID('dbo.goods_receipt_note_lines', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_vendor_returns_grn_line' AND parent_object_id = OBJECT_ID('dbo.vendor_returns'))
BEGIN
    ALTER TABLE dbo.vendor_returns
        ADD CONSTRAINT fk_vendor_returns_grn_line FOREIGN KEY (grn_line_id) REFERENCES dbo.goods_receipt_note_lines(id);
END;

IF OBJECT_ID('dbo.inventory_items', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_vendor_returns_item' AND parent_object_id = OBJECT_ID('dbo.vendor_returns'))
BEGIN
    ALTER TABLE dbo.vendor_returns
        ADD CONSTRAINT fk_vendor_returns_item FOREIGN KEY (item_id) REFERENCES dbo.inventory_items(id);
END;

IF OBJECT_ID('dbo.purchase_orders', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_vendor_returns_po' AND parent_object_id = OBJECT_ID('dbo.vendor_returns'))
BEGIN
    ALTER TABLE dbo.vendor_returns
        ADD CONSTRAINT fk_vendor_returns_po FOREIGN KEY (po_id) REFERENCES dbo.purchase_orders(id);
END;

IF OBJECT_ID('dbo.purchase_order_lines', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_vendor_returns_po_line' AND parent_object_id = OBJECT_ID('dbo.vendor_returns'))
BEGIN
    ALTER TABLE dbo.vendor_returns
        ADD CONSTRAINT fk_vendor_returns_po_line FOREIGN KEY (po_line_id) REFERENCES dbo.purchase_order_lines(id);
END;
