-- Add shipping destination fields to material_requisitions
IF OBJECT_ID('material_requisitions', 'U') IS NOT NULL AND COL_LENGTH('material_requisitions', 'ship_to_type') IS NULL
BEGIN
    ALTER TABLE material_requisitions ADD ship_to_type NVARCHAR(20) NULL;
END;
GO
IF OBJECT_ID('material_requisitions', 'U') IS NOT NULL AND COL_LENGTH('material_requisitions', 'ship_to_warehouse_id') IS NULL
BEGIN
    ALTER TABLE material_requisitions ADD ship_to_warehouse_id BIGINT NULL;
END;
GO
IF OBJECT_ID('material_requisitions', 'U') IS NOT NULL AND COL_LENGTH('material_requisitions', 'ship_to_work_order_id') IS NULL
BEGIN
    ALTER TABLE material_requisitions ADD ship_to_work_order_id BIGINT NULL;
END;
GO

IF OBJECT_ID('dbo.material_requisitions', 'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.indexes
       WHERE name = 'idx_mr_ship_to_type'
         AND object_id = OBJECT_ID('dbo.material_requisitions')
   )
BEGIN
    CREATE INDEX idx_mr_ship_to_type ON dbo.material_requisitions(ship_to_type);
END;
GO

IF OBJECT_ID('dbo.material_requisitions', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.warehouses', 'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.foreign_keys
       WHERE name = 'fk_mr_ship_to_wh'
         AND parent_object_id = OBJECT_ID('dbo.material_requisitions')
   )
BEGIN
    ALTER TABLE dbo.material_requisitions
        ADD CONSTRAINT fk_mr_ship_to_wh FOREIGN KEY (ship_to_warehouse_id) REFERENCES dbo.warehouses(id);
END;
GO

IF OBJECT_ID('dbo.material_requisitions', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.work_orders', 'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.foreign_keys
       WHERE name = 'fk_mr_ship_to_work_order'
         AND parent_object_id = OBJECT_ID('dbo.material_requisitions')
   )
BEGIN
    ALTER TABLE dbo.material_requisitions
        ADD CONSTRAINT fk_mr_ship_to_work_order FOREIGN KEY (ship_to_work_order_id) REFERENCES dbo.work_orders(id);
END;
GO

