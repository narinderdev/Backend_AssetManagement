-- Migrate inventory items to use warehouses directly and remove inventory_locations table

-- Add warehouse_id to inventory_items if missing
IF COL_LENGTH('inventory_items', 'warehouse_id') IS NULL
BEGIN
    ALTER TABLE inventory_items ADD warehouse_id BIGINT NULL;
END;
GO

-- Move location_id data to warehouse_id when possible (assumes inventory_locations already linked to warehouses)
UPDATE ii
SET warehouse_id = il.warehouse_id
FROM inventory_items ii
JOIN inventory_locations il ON ii.location_id = il.id
WHERE ii.warehouse_id IS NULL;
GO

-- Drop FK/index on location_id if present
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_inventory_items_location')
BEGIN
    ALTER TABLE inventory_items DROP CONSTRAINT fk_inventory_items_location;
END;
GO

IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_inventory_items_location' AND object_id = OBJECT_ID('inventory_items'))
BEGIN
    DROP INDEX idx_inventory_items_location ON inventory_items;
END;
GO

-- Drop location_id column
IF COL_LENGTH('inventory_items', 'location_id') IS NOT NULL
BEGIN
    ALTER TABLE inventory_items DROP COLUMN location_id;
END;
GO

-- Add FK/index on warehouse_id
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_inventory_items_warehouse')
BEGIN
    ALTER TABLE inventory_items
        ADD CONSTRAINT fk_inventory_items_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id);
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_inventory_items_warehouse' AND object_id = OBJECT_ID('inventory_items'))
BEGIN
    CREATE INDEX idx_inventory_items_warehouse ON inventory_items(warehouse_id);
END;
GO

-- Drop inventory_locations table (and related indexes) if it exists
IF EXISTS (SELECT 1 FROM sys.tables WHERE name = 'inventory_locations')
BEGIN
    DROP TABLE inventory_locations;
END;
GO
