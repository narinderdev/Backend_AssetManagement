-- Migrate inventory items to use warehouses directly and remove inventory_locations table

-- Add warehouse_id to inventory_items if missing
IF OBJECT_ID('dbo.inventory_items', 'U') IS NOT NULL
   AND COL_LENGTH('dbo.inventory_items', 'warehouse_id') IS NULL
BEGIN
    ALTER TABLE dbo.inventory_items ADD warehouse_id BIGINT NULL;
END;
GO

-- Move location_id data to warehouse_id when possible (assumes inventory_locations already linked to warehouses)
IF OBJECT_ID('dbo.inventory_items', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.inventory_locations', 'U') IS NOT NULL
BEGIN
    UPDATE ii
    SET warehouse_id = il.warehouse_id
    FROM dbo.inventory_items ii
    JOIN dbo.inventory_locations il ON ii.location_id = il.id
    WHERE ii.warehouse_id IS NULL;
END;
GO

-- Drop FK/index on location_id if present
IF OBJECT_ID('dbo.inventory_items', 'U') IS NOT NULL
   AND EXISTS (
       SELECT 1
       FROM sys.foreign_keys
       WHERE name = 'fk_inventory_items_location'
         AND parent_object_id = OBJECT_ID('dbo.inventory_items')
   )
BEGIN
    ALTER TABLE dbo.inventory_items DROP CONSTRAINT fk_inventory_items_location;
END;
GO

IF OBJECT_ID('dbo.inventory_items', 'U') IS NOT NULL
   AND EXISTS (
       SELECT 1
       FROM sys.indexes
       WHERE name = 'idx_inventory_items_location'
         AND object_id = OBJECT_ID('dbo.inventory_items')
   )
BEGIN
    DROP INDEX idx_inventory_items_location ON dbo.inventory_items;
END;
GO

-- Drop location_id column
IF OBJECT_ID('dbo.inventory_items', 'U') IS NOT NULL
   AND COL_LENGTH('dbo.inventory_items', 'location_id') IS NOT NULL
BEGIN
    ALTER TABLE dbo.inventory_items DROP COLUMN location_id;
END;
GO

-- Add FK/index on warehouse_id
IF OBJECT_ID('dbo.inventory_items', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.warehouses', 'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.foreign_keys
       WHERE name = 'fk_inventory_items_warehouse'
         AND parent_object_id = OBJECT_ID('dbo.inventory_items')
   )
BEGIN
    ALTER TABLE dbo.inventory_items
        ADD CONSTRAINT fk_inventory_items_warehouse FOREIGN KEY (warehouse_id) REFERENCES dbo.warehouses(id);
END;
GO

IF OBJECT_ID('dbo.inventory_items', 'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.indexes
       WHERE name = 'idx_inventory_items_warehouse'
         AND object_id = OBJECT_ID('dbo.inventory_items')
   )
BEGIN
    CREATE INDEX idx_inventory_items_warehouse ON dbo.inventory_items(warehouse_id);
END;
GO

-- Drop inventory_locations table (and related indexes) if it exists
IF EXISTS (SELECT 1 FROM sys.tables WHERE name = 'inventory_locations')
BEGIN
    DROP TABLE inventory_locations;
END;
GO
