IF OBJECT_ID('dbo.inventory_audit_logs', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.inventory_audit_logs (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        inventory_item_id BIGINT NOT NULL,
        transaction_type NVARCHAR(16) NOT NULL,
        reference_type NVARCHAR(32) NOT NULL,
        reference_number NVARCHAR(128) NULL,
        before_quantity INT NOT NULL,
        after_quantity INT NOT NULL,
        variance_quantity INT NOT NULL,
        performed_by NVARCHAR(150) NULL,
        reason NVARCHAR(500) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_inventory_audit_logs_created DEFAULT SYSUTCDATETIME()
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_inv_audit_item' AND object_id = OBJECT_ID('dbo.inventory_audit_logs'))
BEGIN
    CREATE INDEX idx_inv_audit_item ON dbo.inventory_audit_logs(inventory_item_id);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_inv_audit_txn_type' AND object_id = OBJECT_ID('dbo.inventory_audit_logs'))
BEGIN
    CREATE INDEX idx_inv_audit_txn_type ON dbo.inventory_audit_logs(transaction_type);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_inv_audit_ref_type' AND object_id = OBJECT_ID('dbo.inventory_audit_logs'))
BEGIN
    CREATE INDEX idx_inv_audit_ref_type ON dbo.inventory_audit_logs(reference_type);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_inv_audit_created' AND object_id = OBJECT_ID('dbo.inventory_audit_logs'))
BEGIN
    CREATE INDEX idx_inv_audit_created ON dbo.inventory_audit_logs(created_at);
END;

IF OBJECT_ID('dbo.inventory_items', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_inv_audit_item' AND parent_object_id = OBJECT_ID('dbo.inventory_audit_logs'))
BEGIN
    ALTER TABLE dbo.inventory_audit_logs
    ADD CONSTRAINT fk_inv_audit_item FOREIGN KEY (inventory_item_id) REFERENCES dbo.inventory_items(id);
END;
