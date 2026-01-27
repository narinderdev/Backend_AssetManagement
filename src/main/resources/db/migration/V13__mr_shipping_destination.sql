-- Add shipping destination fields to material_requisitions
IF COL_LENGTH('material_requisitions', 'ship_to_type') IS NULL
BEGIN
    ALTER TABLE material_requisitions ADD ship_to_type NVARCHAR(20) NULL;
END;
GO

IF COL_LENGTH('material_requisitions', 'ship_to_warehouse_id') IS NULL
BEGIN
    ALTER TABLE material_requisitions ADD ship_to_warehouse_id BIGINT NULL;
END;
GO

IF COL_LENGTH('material_requisitions', 'ship_to_work_order_id') IS NULL
BEGIN
    ALTER TABLE material_requisitions ADD ship_to_work_order_id BIGINT NULL;
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_mr_ship_to_type' AND object_id = OBJECT_ID('material_requisitions'))
BEGIN
    CREATE INDEX idx_mr_ship_to_type ON material_requisitions(ship_to_type);
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_mr_ship_to_wh')
BEGIN
    ALTER TABLE material_requisitions
        ADD CONSTRAINT fk_mr_ship_to_wh FOREIGN KEY (ship_to_warehouse_id) REFERENCES warehouses(id);
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_mr_ship_to_work_order')
BEGIN
    ALTER TABLE material_requisitions
        ADD CONSTRAINT fk_mr_ship_to_work_order FOREIGN KEY (ship_to_work_order_id) REFERENCES work_orders(id);
END;
GO
