-- Add bin/zone fields directly on warehouses for high-level storage metadata
IF COL_LENGTH('warehouses', 'zone_aisle') IS NULL
BEGIN
    ALTER TABLE warehouses ADD zone_aisle NVARCHAR(128) NULL;
END;
GO

IF COL_LENGTH('warehouses', 'rack_shelf') IS NULL
BEGIN
    ALTER TABLE warehouses ADD rack_shelf NVARCHAR(128) NULL;
END;
GO

IF COL_LENGTH('warehouses', 'bin_code') IS NULL
BEGIN
    ALTER TABLE warehouses ADD bin_code NVARCHAR(128) NULL;
END;
GO

IF COL_LENGTH('warehouses', 'bin_description') IS NULL
BEGIN
    ALTER TABLE warehouses ADD bin_description NVARCHAR(512) NULL;
END;
GO
