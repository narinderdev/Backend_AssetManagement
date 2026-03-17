IF OBJECT_ID('dbo.inventory_reconciliations', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.inventory_reconciliations (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        warehouse_id BIGINT NOT NULL,
        inventory_item_id BIGINT NOT NULL,
        reconcile_date DATE NOT NULL,
        entered_by NVARCHAR(150) NULL,
        system_quantity INT NOT NULL,
        physical_quantity INT NOT NULL,
        variance_quantity INT NOT NULL,
        cost_per_unit_snapshot DECIMAL(19,2) NULL,
        variance_cost DECIMAL(19,2) NULL,
        status NVARCHAR(16) NOT NULL,
        reason NVARCHAR(500) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_inventory_reconciliations_created DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT df_inventory_reconciliations_updated DEFAULT SYSUTCDATETIME()
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_invrec_warehouse' AND object_id = OBJECT_ID('dbo.inventory_reconciliations'))
BEGIN
    CREATE INDEX idx_invrec_warehouse ON dbo.inventory_reconciliations(warehouse_id);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_invrec_item' AND object_id = OBJECT_ID('dbo.inventory_reconciliations'))
BEGIN
    CREATE INDEX idx_invrec_item ON dbo.inventory_reconciliations(inventory_item_id);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_invrec_status' AND object_id = OBJECT_ID('dbo.inventory_reconciliations'))
BEGIN
    CREATE INDEX idx_invrec_status ON dbo.inventory_reconciliations(status);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_invrec_date' AND object_id = OBJECT_ID('dbo.inventory_reconciliations'))
BEGIN
    CREATE INDEX idx_invrec_date ON dbo.inventory_reconciliations(reconcile_date);
END;

IF OBJECT_ID('dbo.warehouses', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_invrec_wh' AND parent_object_id = OBJECT_ID('dbo.inventory_reconciliations'))
BEGIN
    ALTER TABLE dbo.inventory_reconciliations
    ADD CONSTRAINT fk_invrec_wh FOREIGN KEY (warehouse_id) REFERENCES dbo.warehouses(id);
END;

IF OBJECT_ID('dbo.inventory_items', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_invrec_item' AND parent_object_id = OBJECT_ID('dbo.inventory_reconciliations'))
BEGIN
    ALTER TABLE dbo.inventory_reconciliations
    ADD CONSTRAINT fk_invrec_item FOREIGN KEY (inventory_item_id) REFERENCES dbo.inventory_items(id);
END;
