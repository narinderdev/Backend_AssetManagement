-- Create warehouses table
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'warehouses')
BEGIN
    CREATE TABLE warehouses (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        name NVARCHAR(255) NOT NULL,
        active BIT NOT NULL CONSTRAINT df_warehouses_active DEFAULT 1,
        deleted BIT NOT NULL CONSTRAINT df_warehouses_deleted DEFAULT 0,
        created_at DATETIME2 NOT NULL CONSTRAINT df_warehouses_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT df_warehouses_updated_at DEFAULT SYSUTCDATETIME()
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_warehouses_name' AND object_id = OBJECT_ID('warehouses'))
BEGIN
    CREATE UNIQUE INDEX ux_warehouses_name ON warehouses(name);
END;
GO

-- Create inventory_locations table
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'inventory_locations')
BEGIN
    CREATE TABLE inventory_locations (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        warehouse_id BIGINT NOT NULL,
        zone_aisle NVARCHAR(128) NULL,
        rack_shelf NVARCHAR(128) NULL,
        bin_code NVARCHAR(128) NULL,
        bin_description NVARCHAR(512) NULL,
        active BIT NOT NULL CONSTRAINT df_inventory_locations_active DEFAULT 1,
        deleted BIT NOT NULL CONSTRAINT df_inventory_locations_deleted DEFAULT 0,
        created_at DATETIME2 NOT NULL CONSTRAINT df_inventory_locations_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT df_inventory_locations_updated_at DEFAULT SYSUTCDATETIME()
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_inventory_locations_warehouse')
BEGIN
    ALTER TABLE inventory_locations
        ADD CONSTRAINT fk_inventory_locations_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id);
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_inventory_locations_warehouse' AND object_id = OBJECT_ID('inventory_locations'))
BEGIN
    CREATE INDEX idx_inventory_locations_warehouse ON inventory_locations(warehouse_id);
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_inventory_locations_active' AND object_id = OBJECT_ID('inventory_locations'))
BEGIN
    CREATE INDEX idx_inventory_locations_active ON inventory_locations(active, deleted);
END;
GO

-- Add SKU and location references to inventory_items
IF COL_LENGTH('inventory_items', 'sku_number') IS NULL
BEGIN
    ALTER TABLE inventory_items ADD sku_number NVARCHAR(128) NULL;
END;
GO

IF COL_LENGTH('inventory_items', 'location_id') IS NULL
BEGIN
    ALTER TABLE inventory_items ADD location_id BIGINT NULL;
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_inventory_items_location')
BEGIN
    ALTER TABLE inventory_items
        ADD CONSTRAINT fk_inventory_items_location FOREIGN KEY (location_id) REFERENCES inventory_locations(id);
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_inventory_items_location' AND object_id = OBJECT_ID('inventory_items'))
BEGIN
    CREATE INDEX idx_inventory_items_location ON inventory_items(location_id);
END;
GO

-- Enforce SKU uniqueness for non-deleted rows
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_inventory_items_sku_active' AND object_id = OBJECT_ID('inventory_items'))
BEGIN
    CREATE UNIQUE INDEX ux_inventory_items_sku_active
        ON inventory_items(sku_number)
        WHERE sku_number IS NOT NULL AND deleted = 0;
END;
GO
